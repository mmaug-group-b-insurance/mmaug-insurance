[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$JdkHome,

    [ValidateSet('compile', 'test')]
    [string]$Goal = 'test'
)

$ErrorActionPreference = 'Stop'
$taskJdkPath = (Resolve-Path -LiteralPath $JdkHome).Path
$taskCompiler = Join-Path $taskJdkPath 'bin/javac.exe'
if (!(Test-Path -LiteralPath $taskCompiler -PathType Leaf)) {
    throw 'JdkHome must point to a full JDK with bin/javac.exe, not a JRE.'
}
$taskCompilerVersion = (& $taskCompiler -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $taskCompilerVersion -notmatch '^javac (\d+)') {
    throw 'Could not determine the JDK compiler version.'
}
if ([int]$Matches[1] -lt 17) {
    throw 'This backend requires JDK 17 or a team-approved compatible newer JDK.'
}

$taskBackend = Join-Path $PSScriptRoot '../mmaug-claims-agent-backend'
$taskOldJavaHome = $env:JAVA_HOME
$taskOldPath = $env:PATH
$taskExit = 1
Push-Location -LiteralPath $taskBackend
try {
    $env:JAVA_HOME = $taskJdkPath
    $env:PATH = "$(Join-Path $taskJdkPath 'bin');$taskOldPath"
    & .\mvnw.cmd -v
    if ($LASTEXITCODE -ne 0) { throw 'Maven wrapper version check failed.' }
    & .\mvnw.cmd -B -ntp $Goal
    $taskExit = $LASTEXITCODE
}
finally {
    $env:JAVA_HOME = $taskOldJavaHome
    $env:PATH = $taskOldPath
    Pop-Location
}
exit $taskExit
