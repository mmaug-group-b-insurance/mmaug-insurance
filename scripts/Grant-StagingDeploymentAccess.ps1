# An authorized RBAC administrator runs this after review. No secret values are used.
[CmdletBinding(SupportsShouldProcess=$true)]
param()
$ErrorActionPreference = 'Stop'
$subscriptionId = '9a2ab22e-5269-4da3-a16a-eeed2b0c09aa'
$principalId = '863dba30-77e1-49b6-af0d-2d26f7804bc2'
$accountJson = az account show -o json --only-show-errors
if ($LASTEXITCODE -ne 0) { throw 'Azure CLI sign-in is required.' }
$account = $accountJson | ConvertFrom-Json
if ($account.id -ne $subscriptionId -or $account.tenantId -ne '71beefd7-29f4-4b9e-9cb2-e78b5dc851ad') {
    throw 'Select the project subscription and tenant first.'
}
$actualId = az identity show -g rg_mmaug_insurance -n id-mmaug-github-stg --query principalId -o tsv --only-show-errors
if ($LASTEXITCODE -ne 0 -or $actualId -ne $principalId) { throw 'Deployment identity differs from reviewed principal.' }
foreach ($app in @('app-mmaug-api-stg-01','app-mmaug-web-stg-01')) {
    $scope = "/subscriptions/$subscriptionId/resourceGroups/rg_mmaug_insurance/providers/Microsoft.Web/sites/$app"
    if ($PSCmdlet.ShouldProcess($scope, "Grant Website Contributor to deployment identity $principalId")) {
        az role assignment create --subscription $subscriptionId --assignee-object-id $principalId `
            --assignee-principal-type ServicePrincipal --role 'Website Contributor' --scope $scope `
            --only-show-errors --output none
        if ($LASTEXITCODE -ne 0) { throw "Role assignment failed for $app. Earlier successful grants remain." }
        Write-Output "Granted Website Contributor at $scope"
    }
}
