[CmdletBinding()]
param([string]$JdkHome='C:\Users\fumeh\.cache\mmaug-runtimes\jdk-17.0.20.1+1',[switch]$KeepDatabase)
$ErrorActionPreference='Stop'
$pgBin=Join-Path $env:ProgramFiles 'PostgreSQL/18/bin'
$testDatabase='mmaug_sprint_test_'+[Guid]::NewGuid().ToString('N')
$baselineDatabase='mmaug_sprint_test_'+[Guid]::NewGuid().ToString('N')
if($testDatabase -notmatch '^mmaug_sprint_test_[a-f0-9]{32}$'){throw 'Unsafe test database name'}
$oldValues=@{}
foreach($name in @('PGPASSWORD','JAVA_HOME','PATH','MMAUG_INTEGRATION_DB_URL','MMAUG_BASELINE_DB_URL','MMAUG_INTEGRATION_DB_USER','MMAUG_INTEGRATION_DB_PASSWORD','MMAUG_RUN_LIVE_OCR','MMAUG_RUN_LIVE_MODEL','SPRING_APPLICATION_JSON')){$oldValues[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
$createdDatabases=@()
Push-Location (Join-Path $PSScriptRoot '../mmaug-claims-agent-backend')
try{
 $env:PGPASSWORD=(Get-Content -LiteralPath (Join-Path $env:USERPROFILE '.cache/mmaug-preview/postgres-native/password.txt') -Raw).Trim()
 & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -T template0 $testDatabase
 if($LASTEXITCODE -ne 0){throw 'Isolated test database creation failed'}
 $createdDatabases+=,$testDatabase
 & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -T template0 $baselineDatabase
 if($LASTEXITCODE -ne 0){throw 'Isolated baseline database creation failed'}
 $createdDatabases+=,$baselineDatabase
 $env:MMAUG_INTEGRATION_DB_URL="jdbc:postgresql://127.0.0.1:55432/$testDatabase"
 $env:MMAUG_BASELINE_DB_URL="jdbc:postgresql://127.0.0.1:55432/$baselineDatabase"
 $env:MMAUG_INTEGRATION_DB_USER='mmaug_preview';$env:MMAUG_INTEGRATION_DB_PASSWORD=$env:PGPASSWORD
 $env:JAVA_HOME=$JdkHome;$env:PATH="$JdkHome\bin;$env:PATH"
 $env:MMAUG_RUN_LIVE_OCR='false';$env:MMAUG_RUN_LIVE_MODEL='false';$env:SPRING_APPLICATION_JSON=$null
 Write-Host "Running synthetic workflow checks in isolated database $testDatabase; OCR is mocked."
 & .\mvnw.cmd -B -ntp test
 if($LASTEXITCODE -ne 0){throw 'Sprint tests failed; inspect target/surefire-reports.'}
}finally{
 if(!$KeepDatabase){foreach($createdDatabase in $createdDatabases){
  if($createdDatabase -notmatch '^mmaug_sprint_test_[a-f0-9]{32}$'){throw 'Unsafe database cleanup target'}
  & (Join-Path $pgBin 'dropdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview --force $createdDatabase
  if($LASTEXITCODE -ne 0){Write-Warning "Could not remove isolated test database $createdDatabase"}
 }
 }
 foreach($name in $oldValues.Keys){[Environment]::SetEnvironmentVariable($name,$oldValues[$name],'Process')}
 Pop-Location
}
