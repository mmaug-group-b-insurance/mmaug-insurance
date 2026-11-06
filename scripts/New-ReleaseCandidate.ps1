[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$OutputDirectory,
      [string]$VerificationFile='docs/pii-test-results-2026-11-06.json')
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jar=Join-Path $repo 'mmaug-claims-agent-backend/target/mmaug-claims-agent-backend-0.0.1-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Build the executable JAR after passing tests first'}
$verification=Get-Content -LiteralPath (Join-Path $repo $VerificationFile) -Raw | ConvertFrom-Json
if($verification.jarSha256 -ne (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash){throw 'Verification evidence does not match the current JAR'}
if(Test-Path -LiteralPath $OutputDirectory){throw 'Use a new release output directory'}
$destination=[IO.Path]::GetFullPath($OutputDirectory)
$repoPrefix=$repo.TrimEnd('\')+'\'
if($destination.StartsWith($repoPrefix,[StringComparison]::OrdinalIgnoreCase)){throw 'Release outputs must be outside the shared repository'}
New-Item -ItemType Directory -Path $destination | Out-Null
# Explicit allowlist: never recursively copy source, .env, storage, database dumps or credentials.
$files=@(
 'mmaug-claims-agent-backend/target/mmaug-claims-agent-backend-0.0.1-SNAPSHOT.jar',
 'docs/release-readiness-2026-11-05.md',
 $VerificationFile,
 'docs/private-evidence-increment-2026-11-05.md',
 'docs/pii-release-increment-2026-11-06.md',
 'docs/ci-container-checkpoint-2026-11-06.md',
 'docs/container-test-results-2026-11-06.json',
 'docs/live-evaluation-results-2026-11-05.json',
 'docs/business-review-acceptance-2026-11-05.md',
 'docs/provider-transaction-test-results-2026-11-05.json',
 'docs/v16-live-acceptance-2026-11-05.json',
 'infra/staging/staging.env.example'
)
$manifest=@()
foreach($file in $files){
 $source=Join-Path $repo $file
 if(!(Test-Path -LiteralPath $source)){throw "Required release evidence missing: $file"}
 $target=Join-Path $destination ([IO.Path]::GetFileName($file))
 Copy-Item -LiteralPath $source -Destination $target
 $manifest+=,[ordered]@{file=[IO.Path]::GetFileName($target);sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash;bytes=(Get-Item -LiteralPath $target).Length}
}
$head=& git -C $repo rev-parse HEAD
if($LASTEXITCODE -ne 0){throw 'Git revision lookup failed'}
$dirty=@(& git -C $repo status --porcelain).Count -gt 0
[ordered]@{createdAtUtc=[DateTime]::UtcNow.ToString('o');baseCommit=[string]$head;uncommittedChanges=$dirty;candidateOnly=$true;deployed=$false;files=$manifest} | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $destination 'manifest.json') -Encoding UTF8
Write-Host "Candidate assembled at $destination. Verify manifest hashes before transferring. Not a deployable staging acceptance."
