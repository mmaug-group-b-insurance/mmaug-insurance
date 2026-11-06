[CmdletBinding()]
param([string]$OutputDirectory=(Join-Path $env:USERPROFILE ('.cache/mmaug-preview/restore-'+[Guid]::NewGuid().ToString('N'))))
$ErrorActionPreference='Stop'
# This rehearsal reads only the fixed synthetic preview, and restores only into a new random database.
$pgBin=Join-Path $env:ProgramFiles 'PostgreSQL/18/bin'
$restoreDatabase='mmaug_restore_test_'+[Guid]::NewGuid().ToString('N')
if($restoreDatabase -notmatch '^mmaug_restore_test_[a-f0-9]{32}$'){throw 'Unsafe restore target'}
if(Test-Path -LiteralPath $OutputDirectory){throw 'Use a new output directory'}
$oldPassword=[Environment]::GetEnvironmentVariable('PGPASSWORD','Process')
$created=$false
$tables=@('claim','claim_document','extraction_result','agent_run','claim_summary','officer_review','validation_finding')
function Read-Fingerprints([string]$Database){
 $result=[ordered]@{}
 foreach($table in $tables){
  # Names are fixed above; digest is retained, no document or reviewer content is emitted.
  $sql="select count(*)::text || ':' || md5(coalesce(string_agg(md5(t::text),'' order by id),'')) from $table t"
  $value=& (Join-Path $pgBin 'psql.exe') -X -h 127.0.0.1 -p 55432 -U mmaug_preview -d $Database -At -v ON_ERROR_STOP=1 -c $sql
  if($LASTEXITCODE -ne 0){throw 'Database fingerprint failed'}
  $result[$table]=[string]$value
 }
 return ($result | ConvertTo-Json -Compress)
}
try{
 New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
 $env:PGPASSWORD=(Get-Content -LiteralPath (Join-Path $env:USERPROFILE '.cache/mmaug-preview/postgres-native/password.txt') -Raw).Trim()
 $before=Read-Fingerprints 'mmaug_claims'
 $dumpPath=Join-Path $OutputDirectory 'synthetic-preview.dump'
 & (Join-Path $pgBin 'pg_dump.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -d mmaug_claims -Fc --no-owner --no-acl -f $dumpPath
 if($LASTEXITCODE -ne 0){throw 'Synthetic backup failed'}
 & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -T template0 $restoreDatabase
 if($LASTEXITCODE -ne 0){throw 'Isolated restore database creation failed'}
 $created=$true
 & (Join-Path $pgBin 'pg_restore.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview -d $restoreDatabase --no-owner --no-acl --exit-on-error $dumpPath
 if($LASTEXITCODE -ne 0){throw 'Isolated restore failed'}
 $restored=Read-Fingerprints $restoreDatabase
 $after=Read-Fingerprints 'mmaug_claims'
 if($before -ne $after){throw 'Preview changed during rehearsal; no stable comparison claimed. Retry during a quiet window.'}
 if($before -ne $restored){throw 'Restored records differ from synthetic source'}
 $migration=& (Join-Path $pgBin 'psql.exe') -X -h 127.0.0.1 -p 55432 -U mmaug_preview -d $restoreDatabase -At -v ON_ERROR_STOP=1 -c "select version from flyway_schema_history where success and version is not null order by installed_rank desc limit 1"
 if($LASTEXITCODE -ne 0){throw 'Restored migration lookup failed'}
 [ordered]@{testedAtUtc=[DateTime]::UtcNow.ToString('o');source='local synthetic preview';isolatedRestore=$true;sourceUnchanged=$true;allComparedRowsMatched=$true;tableFingerprints=($before|ConvertFrom-Json);flywayVersion=[string]$migration;dumpSha256=(Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash;scope='PostgreSQL only; evidence files and Azure disaster recovery not exercised'} | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'restore-report.json') -Encoding UTF8
 Write-Host "Synthetic database restore comparison passed. Report: $(Join-Path $OutputDirectory 'restore-report.json')"
}finally{
 if($created){
  if($restoreDatabase -notmatch '^mmaug_restore_test_[a-f0-9]{32}$'){throw 'Unsafe restore cleanup target'}
  & (Join-Path $pgBin 'dropdb.exe') -h 127.0.0.1 -p 55432 -U mmaug_preview --force $restoreDatabase
  if($LASTEXITCODE -ne 0){Write-Warning 'Isolated restore database cleanup failed'}
 }
 [Environment]::SetEnvironmentVariable('PGPASSWORD',$oldPassword,'Process')
}
