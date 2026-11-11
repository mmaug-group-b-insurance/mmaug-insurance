# Run by an administrator authorized to assign RBAC roles at the listed resource scopes.
# No credentials are read, and no resource or subscription-wide access is granted.
[CmdletBinding(SupportsShouldProcess=$true)]
param()
$ErrorActionPreference = 'Stop'
$subscriptionId = '9a2ab22e-5269-4da3-a16a-eeed2b0c09aa'
$tenantId = '71beefd7-29f4-4b9e-9cb2-e78b5dc851ad'
$principalId = '7110205a-5600-4d19-bff1-de24182309d2'
$resourceBase = "/subscriptions/$subscriptionId/resourceGroups/rg_mmaug_insurance"
$accountJson = az account show --only-show-errors -o json
if ($LASTEXITCODE -ne 0) { throw 'Azure CLI sign-in is required.' }
$account = $accountJson | ConvertFrom-Json
if ($account.id -ne $subscriptionId -or $account.tenantId -ne $tenantId) {
    throw 'Select the project subscription and tenant before running this script.'
}
$identity = az webapp identity show --subscription $subscriptionId -g rg_mmaug_insurance `
    -n app-mmaug-api-stg-01 --query principalId -o tsv --only-show-errors
if ($LASTEXITCODE -ne 0 -or $identity -ne $principalId) {
    throw 'Backend identity differs from the reviewed principal. Review before granting access.'
}
$grants = @(
    @{ Role='Key Vault Secrets User'; Scope="$resourceBase/providers/Microsoft.KeyVault/vaults/kv-mmaug-claims-stg-01" },
    @{ Role='Storage Blob Data Contributor'; Scope="$resourceBase/providers/Microsoft.Storage/storageAccounts/stmmaugclaimsstg01/blobServices/default/containers/quarantine" },
    @{ Role='Storage Blob Data Contributor'; Scope="$resourceBase/providers/Microsoft.Storage/storageAccounts/stmmaugclaimsstg01/blobServices/default/containers/clean" },
    @{ Role='Storage Queue Data Message Processor'; Scope="$resourceBase/providers/Microsoft.Storage/storageAccounts/stmmaugclaimsstg01/queueServices/default/queues/defender-results" }
)
foreach ($grant in $grants) {
    if ($PSCmdlet.ShouldProcess($grant.Scope, "Grant $($grant.Role) to backend identity $principalId")) {
        az role assignment create --subscription $subscriptionId --assignee-object-id $principalId `
            --assignee-principal-type ServicePrincipal --role $grant.Role --scope $grant.Scope `
            --only-show-errors --output none
        if ($LASTEXITCODE -ne 0) { throw "Role assignment failed: $($grant.Role). Earlier successful grants remain in place." }
        Write-Output "Granted $($grant.Role) at $($grant.Scope)"
    }
}
