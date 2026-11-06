[CmdletBinding()]
param(
 [string]$JdkHome = 'C:\Users\fumeh\.cache\mmaug-runtimes\jdk-17.0.20.1+1',
 [string]$DbUrl,
 [string]$DbUsername,
 [switch]$UseExistingSyntheticDatabase,
 [int]$Port = 8081
)
$ErrorActionPreference='Stop'
if ([string]::IsNullOrWhiteSpace($env:DOCINTEL_KEY)) {
 $ocrSecret=Read-Host 'Paste Document Intelligence Key 1 (hidden)' -AsSecureString
 $env:DOCINTEL_KEY=[System.Net.NetworkCredential]::new('', $ocrSecret).Password
 Remove-Variable ocrSecret
 # Windows PowerShell 5.1 rejects an empty clipboard value. Cleanup must not abort startup.
 try { Set-Clipboard -Value ' ' -ErrorAction Stop } catch { Write-Warning 'Clipboard cleanup failed; clear it manually.' }
}
if ($UseExistingSyntheticDatabase) {
 if ($DbUrl -or $DbUsername) { throw 'Choose the existing synthetic database or explicit database settings, not both.' }
 $previewRoot=Join-Path $env:USERPROFILE '.cache/mmaug-preview/postgres-native'
 if (!(Test-Path -LiteralPath (Join-Path $previewRoot 'data/PG_VERSION'))) { throw 'Existing synthetic preview cluster is missing.' }
 $DbUrl='jdbc:postgresql://127.0.0.1:55432/mmaug_claims'
 $DbUsername='mmaug_preview'
 $env:DB_PASSWORD=(Get-Content -LiteralPath (Join-Path $previewRoot 'password.txt') -Raw).Trim()
 $dbProbe=[System.Net.Sockets.TcpClient]::new()
 try {
  $connection=$dbProbe.ConnectAsync('127.0.0.1',55432)
  if (!$connection.Wait(2000)) { throw 'Connection timed out' }
 } catch {
  $pgCtl=Join-Path $env:ProgramFiles 'PostgreSQL/18/bin/pg_ctl.exe'
  if (!(Test-Path -LiteralPath $pgCtl)) { throw 'PostgreSQL 18 pg_ctl.exe was not found.' }
  # A separate hidden console prevents backend Ctrl+C from reaching PostgreSQL.
  $pgArguments='-D "{0}" -l "{1}" -o "-p 55432 -h 127.0.0.1" -w -t 20 start' -f (Join-Path $previewRoot 'data'),(Join-Path $previewRoot 'server.log')
  Write-Host 'Starting the existing synthetic database in a separate background console...'
  $pgStart=Start-Process -FilePath $pgCtl -ArgumentList $pgArguments -WindowStyle Hidden -PassThru
  if (!$pgStart.WaitForExit(25000)) { throw 'Database startup timed out; inspect the preview PostgreSQL server.log.' }
  if ($pgStart.ExitCode -ne 0) { throw 'Database startup failed; inspect the preview PostgreSQL server.log.' }
  $readyProbe=[System.Net.Sockets.TcpClient]::new()
  try {
   $ready=$readyProbe.ConnectAsync('127.0.0.1',55432)
   if (!$ready.Wait(2000)) { throw 'Database did not become reachable on 55432.' }
  } finally { $readyProbe.Dispose() }
 } finally { $dbProbe.Dispose() }
}
if (!$DbUrl -or !$DbUsername) { throw 'Supply database settings or -UseExistingSyntheticDatabase.' }
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw 'Configure DB_PASSWORD securely in this PowerShell process first. Use an isolated synthetic preview database.' }
if (!(Test-Path -LiteralPath (Join-Path $JdkHome 'bin/javac.exe'))) { throw 'A full JDK 17 is required.' }
$oldJson=$env:SPRING_APPLICATION_JSON
$oldJava=$env:JAVA_HOME
$oldPath=$env:PATH
$settings=@{
 'spring.profiles.active'='local'
 'spring.datasource.url'=$DbUrl
 'spring.datasource.username'=$DbUsername
 'spring.datasource.password'=$env:DB_PASSWORD
 'server.address'='127.0.0.1'
 'server.port'=$Port
 'app.docintel.provider'='azure'
 'app.docintel.endpoint'='https://mmaug-claims-docintel-01.cognitiveservices.azure.com'
 'app.storage.base-path'=(Join-Path $env:USERPROFILE '.cache/mmaug-preview/native-storage').Replace('\','/')
 'logging.level.org.hibernate.SQL'='WARN'
 'spring.jpa.show-sql'=$false
}
Push-Location (Join-Path $PSScriptRoot '../mmaug-claims-agent-backend')
try {
 $env:JAVA_HOME=$JdkHome
 $env:PATH="$JdkHome\bin;$oldPath"
 $env:SPRING_APPLICATION_JSON=$settings | ConvertTo-Json -Compress
 Write-Host 'Starting loopback-only synthetic preview with real Azure OCR. Local authentication and scanner remain development stubs.'
 & .\mvnw.cmd -B -ntp spring-boot:run
 if ($LASTEXITCODE -ne 0) { throw 'Backend stopped with an error. Read the first ERROR or Caused by message in the Maven output above this line; do not share credentials.' }
} finally {
 $env:SPRING_APPLICATION_JSON=$oldJson
 $env:JAVA_HOME=$oldJava
 $env:PATH=$oldPath
 Pop-Location
}
