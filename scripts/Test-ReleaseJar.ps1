[CmdletBinding()]
param([string]$JdkHome='C:\Users\fumeh\.cache\mmaug-runtimes\jdk-17.0.20.1+1',
      [int]$Port=18082,
      [switch]$RestoreSyntheticSnapshot,
      [Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
if(Test-Path -LiteralPath $OutputDirectory){throw 'Use a fresh smoke output directory'}
if($Port -lt 1024 -or $Port -gt 65535){throw 'Invalid smoke port'}
if(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue){throw 'Smoke port already in use'}
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jar=Join-Path $repo 'mmaug-claims-agent-backend/target/mmaug-claims-agent-backend-0.0.1-SNAPSHOT.jar'
$pgBin=Join-Path $env:ProgramFiles 'PostgreSQL/18/bin'
$database='mmaug_release_test_'+[Guid]::NewGuid().ToString('N')
$oldPassword=[Environment]::GetEnvironmentVariable('PGPASSWORD','Process')
$oldJson=[Environment]::GetEnvironmentVariable('SPRING_APPLICATION_JSON','Process')
$created=$false;$candidateProcess=$null
function Historical-Digest {
 $sql="select md5(coalesce(string_agg(v,'' order by v),'')) from (select to_jsonb(t)::text v from claim_summary t union all select to_jsonb(t)::text v from officer_review t union all select to_jsonb(t)::text v from validation_finding t) history"
 $digest=& (Join-Path $pgBin 'psql.exe') -X -h 127.0.0.1 -p 55432 -U mmaug_preview -d $database -At -v ON_ERROR_STOP=1 -c $sql
 if($LASTEXITCODE -ne 0){throw 'Historical acceptance digest failed'}
 return [string]$digest
}
function Status-Code([string]$Path,[string]$Origin='',[string]$Method='GET'){
 $headers=@{}
 if($Origin){$headers['Origin']=$Origin;$headers['Access-Control-Request-Method']='GET'}
 try {return [int](Invoke-WebRequest -Uri "http://127.0.0.1:$Port$Path" -Method $Method -Headers $headers -UseBasicParsing -TimeoutSec 3).StatusCode}
 catch {if($_.Exception.Response){return [int]$_.Exception.Response.StatusCode};return 0}
}
try{
 New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
 $env:PGPASSWORD=(Get-Content -LiteralPath (Join-Path $env:USERPROFILE '.cache/mmaug-preview/postgres-native/password.txt') -Raw).Trim()
 if($database -notmatch '^mmaug_release_test_[a-f0-9]{32}$'){throw 'Unsafe smoke database'}
 & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -T template0 $database
 if($LASTEXITCODE -ne 0){throw 'Smoke database creation failed'}
 $created=$true
 $historicalBefore=$null
 if($RestoreSyntheticSnapshot){
  $snapshotDirectory=Join-Path $env:USERPROFILE '.cache/mmaug-preview/release-restore-20261105'
  $snapshot=Join-Path $snapshotDirectory 'synthetic-preview.dump'
  $evidence=Get-Content -LiteralPath (Join-Path $snapshotDirectory 'restore-report.json') -Raw | ConvertFrom-Json
  if((Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash -ne $evidence.dumpSha256){throw 'Synthetic snapshot checksum mismatch'}
  & (Join-Path $pgBin 'pg_restore.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -d $database --no-owner --no-acl --exit-on-error $snapshot
  if($LASTEXITCODE -ne 0){throw 'Synthetic snapshot import failed'}
  $historicalBefore=Historical-Digest
 }
 $settings=@{
  'spring.profiles.active'='local';'server.address'='127.0.0.1';'server.port'=$Port
  'spring.datasource.url'="jdbc:postgresql://127.0.0.1:55432/$database"
  'spring.datasource.username'='mmaug_preview';'spring.datasource.password'=$env:PGPASSWORD
  'spring.security.oauth2.resourceserver.jwt.issuer-uri'='';'app.security.enabled'=$true
  'app.security.cors.allowed-origins'=@('https://claims.example.test')
  'app.docintel.provider'='stub';'app.ai.summary-provider'='deterministic'
  'app.storage.base-path'=(Join-Path $OutputDirectory 'storage')
 }
 $env:SPRING_APPLICATION_JSON=$settings | ConvertTo-Json -Compress
 $candidateProcess=Start-Process -FilePath (Join-Path $JdkHome 'bin/java.exe') -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $OutputDirectory 'startup.log') -RedirectStandardError (Join-Path $OutputDirectory 'stderr.log')
 [Environment]::SetEnvironmentVariable('SPRING_APPLICATION_JSON',$oldJson,'Process')
 $deadline=[DateTime]::UtcNow.AddSeconds(60)
 do{
  if($candidateProcess.HasExited){throw 'Candidate exited; inspect local smoke startup log'}
  $health=Status-Code '/actuator/health/readiness'
  if($health -eq 200){break}
  Start-Sleep -Milliseconds 500
 }while([DateTime]::UtcNow -lt $deadline)
 if($health -ne 200){throw 'Candidate readiness timeout'}
 $checks=[ordered]@{readiness=$health;anonymousClaims=(Status-Code '/api/v1/claims');anonymousMetrics=(Status-Code '/actuator/metrics');allowedPreflight=(Status-Code '/api/v1/claims' 'https://claims.example.test' 'OPTIONS');deniedPreflight=(Status-Code '/api/v1/claims' 'https://attacker.example.test' 'OPTIONS')}
 if($checks.anonymousClaims -ne 401 -or $checks.anonymousMetrics -ne 401 -or $checks.allowedPreflight -ne 200 -or $checks.deniedPreflight -ne 403){throw 'Candidate security/CORS smoke checks failed'}
 $historyPreserved=$null
 if($RestoreSyntheticSnapshot){$historyPreserved=($historicalBefore -eq (Historical-Digest));if(!$historyPreserved){throw 'Migration changed accepted summary/review/finding history'}}
 $migration=& (Join-Path $pgBin 'psql.exe') -X -h 127.0.0.1 -p 55432 -U mmaug_preview -d $database -At -v ON_ERROR_STOP=1 -c "select version from flyway_schema_history where success and version is not null order by installed_rank desc limit 1"
 if($LASTEXITCODE -ne 0){throw 'Smoke migration lookup failed'}
 [ordered]@{testedAtUtc=[DateTime]::UtcNow.ToString('o');jarSha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash;checks=$checks;flywayVersion=[string]$migration;restoredSyntheticSnapshot=[bool]$RestoreSyntheticSnapshot;acceptedHistoryPreserved=$historyPreserved;scope='Isolated executable JAR/local profile with authentication enabled, no issuer or valid-token flow; not Entra/staging/container acceptance';providerCalls=$false} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'smoke-report.json') -Encoding UTF8
 Write-Host 'Candidate JAR readiness, anonymous rejection and CORS smoke passed.'
}finally{
 if($candidateProcess -and !$candidateProcess.HasExited){Stop-Process -Id $candidateProcess.Id -Force; $candidateProcess.WaitForExit()}
 if($created){
  if($database -notmatch '^mmaug_release_test_[a-f0-9]{32}$'){throw 'Unsafe smoke cleanup target'}
  & (Join-Path $pgBin 'dropdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview --force $database
  if($LASTEXITCODE -ne 0){Write-Warning 'Smoke database cleanup failed'}
 }
 [Environment]::SetEnvironmentVariable('PGPASSWORD',$oldPassword,'Process')
 [Environment]::SetEnvironmentVariable('SPRING_APPLICATION_JSON',$oldJson,'Process')
}
