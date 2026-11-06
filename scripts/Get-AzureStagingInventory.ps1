[CmdletBinding()]
param(
    [string]$SubscriptionId = '9a2ab22e-5269-4da3-a16a-eeed2b0c09aa',
    [string]$ResourceGroup = 'rg_mmaug_insurance'
)
$ErrorActionPreference = 'Stop'
if (!(Get-Command az -ErrorAction SilentlyContinue)) {
    throw 'Azure CLI is required. Sign in using az login before running this read-only inventory.'
}
# Explicit subscription on every call; never change the current CLI account or list keys.
$taskGroup = & az group show --subscription $SubscriptionId --name $ResourceGroup --query '{id:id,name:name,location:location}' -o json --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Resource group lookup failed. Check Azure sign-in and Reader access.' }
$taskResources = & az resource list --subscription $SubscriptionId --resource-group $ResourceGroup --query '[].{name:name,type:type,location:location,kind:kind}' -o json --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Resource inventory failed.' }
[ordered]@{
    inspectedAtUtc = [DateTime]::UtcNow.ToString('o')
    resourceGroup = ($taskGroup | ConvertFrom-Json)
    resources = @($taskResources | ConvertFrom-Json)
} | ConvertTo-Json -Depth 8
