[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
# Read-only metadata. Never lists keys, tokens, queue bodies or claim data.
$subscription='9a2ab22e-5269-4da3-a16a-eeed2b0c09aa'
$group='rg_mmaug_insurance'
$account="/subscriptions/$subscription/resourceGroups/$group/providers/Microsoft.Storage/storageAccounts/stmmaugclaimsstg01"
$topic="/subscriptions/$subscription/resourceGroups/$group/providers/Microsoft.EventGrid/topics/evgt-mmaug-malware-results-stg"
function Read-Azure([string[]]$AzureArguments){
 $json=& az @AzureArguments -o json --only-show-errors
 if($LASTEXITCODE -ne 0){throw 'Azure metadata lookup failed; inspect access to the target resource'}
 return ($json | ConvertFrom-Json)
}
$storage=Read-Azure @('storage','account','show','--ids',$account)
$delivery=Read-Azure @('eventgrid','event-subscription','show','--source-resource-id',$topic,'--name','evgs-mmaug-malware-to-queue-stg')
# Query only safe status properties: raw provider messages can include user identity details.
$scan=Read-Azure @('rest','--method','get','--url',"https://management.azure.com$account/providers/Microsoft.Security/defenderForStorageSettings/current?api-version=2022-12-01-preview",'--query','properties.{enabled:isEnabled,override:overrideSubscriptionLevelSettings,onUpload:malwareScanning.onUpload,resultTopic:malwareScanning.scanResultsEventGridTopicResourceId,operationCode:malwareScanning.operationStatus.code}')
$containers=@(Read-Azure @('storage','container-rm','list','--subscription',$subscription,'-g',$group,'--storage-account','stmmaugclaimsstg01','--query','[].{name:name,publicAccess:publicAccess}'))
$privateContainers=@($containers | Where-Object {$_.name -in @('quarantine','clean','eventgrid-deadletter') -and (!$_.publicAccess -or $_.publicAccess -eq 'None')})
[ordered]@{
 inspectedAtUtc=[DateTime]::UtcNow.ToString('o')
 accountPublicNetworkAccess=$storage.publicNetworkAccess
 anonymousBlobAccessDisabled=($storage.allowBlobPublicAccess -eq $false)
 sharedKeyAccessDisabled=($storage.allowSharedKeyAccess -eq $false)
 privateEndpointConnectionCount=@($storage.privateEndpointConnections).Count
 requiredPrivateContainersPresent=($privateContainers.Count -eq 3)
 eventSubscriptionState=$delivery.provisioningState
 deliveryIdentity=$delivery.deliveryWithResourceIdentity.identity.type
 queue=$delivery.deliveryWithResourceIdentity.destination.queueName
 deadLetterIdentity=$delivery.deadLetterWithResourceIdentity.identity.type
 deadLetterContainer=$delivery.deadLetterWithResourceIdentity.deadLetterDestination.blobContainerName
 eventDeliverySchema=$delivery.eventDeliverySchema
 retryPolicy=$delivery.retryPolicy
 filters=$delivery.filter
 defender=$scan
 scanConfigurationMatchesIntent=($scan.enabled -eq $true -and $scan.override -eq $true -and $scan.onUpload.isEnabled -eq $true -and $scan.onUpload.capGBPerMonth -eq 1 -and $scan.resultTopic -eq $topic)
 liveDeliveryAccepted=$false
 scope='Configuration inspection only; real Defender scan, Base64 decode, queue consumption, monitoring notifications and private connectivity require live acceptance'
} | ConvertTo-Json -Depth 12
