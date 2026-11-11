targetScope = 'resourceGroup'

@description('Primary application and evidence region.')
param appLocation string = 'eastus'

@description('Database region matching the existing PostgreSQL Flexible Server.')
param dbLocation string = 'centralus'

@description('AI Foundry / OpenAI region.')
param aiLocation string = 'swedencentral'

@description('Microsoft Entra tenant ID.')
param tenantId string = '71beefd7-29f4-4b9e-9cb2-e78b5dc851ad'

@description('App Service Plan name.')
param appServicePlanName string = 'asp-mmaug-stg'

@description('Backend API Web App name.')
param apiAppName string = 'app-mmaug-api-stg-01'

@description('Frontend SPA Web App name.')
param webAppName string = 'app-mmaug-web-stg-01'

@description('Private evidence Storage Account name.')
param storageAccountName string = 'stmmaugclaimsstg01'

@description('Key Vault name for runtime secret references.')
param keyVaultName string = 'kv-mmaug-claims-stg-01'

@description('Event Grid system topic name for Defender for Storage malware scan results.')
param malwareTopicName string = 'evgt-mmaug-malware-results-stg'

@description('Azure Document Intelligence account name.')
param docIntelAccountName string = 'mmaug-claims-docintel-01'

@description('Azure AI Foundry / OpenAI account name.')
param aiAccountName string = 'fr-mmaug-insurance'

@description('PostgreSQL Flexible Server name (existing stateful data store; never recreated or overwritten).')
param postgresServerName string = 'pg-mmaug-claims-stg-01'

@description('GitHub Actions OIDC user-assigned managed identity name.')
param deployIdentityName string = 'id-mmaug-github-stg'

@description('Application VNet name in East US.')
param appVnetName string = 'vnet-mmaug-claims-stg-eus'

// ---------------------------------------------------------------------------
// 1. Existing Stateful & Network Foundation References (Data Preservation)
// ---------------------------------------------------------------------------
resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-12-01-preview' existing = {
  name: postgresServerName
}

resource appVnet 'Microsoft.Network/virtualNetworks@2023-11-01' existing = {
  name: appVnetName
}

resource deployIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: deployIdentityName
  location: appLocation
}

// ---------------------------------------------------------------------------
// 2. Compute: App Service Plan, Backend API (Java 17), Frontend SPA (Node 20)
// ---------------------------------------------------------------------------
resource appServicePlan 'Microsoft.Web/serverFarms@2023-12-01' = {
  name: appServicePlanName
  location: appLocation
  kind: 'linux'
  sku: {
    name: 'B1'
    tier: 'Basic'
    capacity: 1
  }
  properties: {
    reserved: true
  }
}

resource apiApp 'Microsoft.Web/sites@2023-12-01' = {
  name: apiAppName
  location: appLocation
  kind: 'app,linux'
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    publicNetworkAccess: 'Enabled'
    vnetRouteAllEnabled: true
    siteConfig: {
      linuxFxVersion: 'JAVA|17-java17'
      alwaysOn: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      http20Enabled: true
      healthCheckPath: '/actuator/health/readiness'
    }
  }
}

resource webApp 'Microsoft.Web/sites@2023-12-01' = {
  name: webAppName
  location: appLocation
  kind: 'app,linux'
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    publicNetworkAccess: 'Enabled'
    siteConfig: {
      linuxFxVersion: 'NODE|20-lts'
      alwaysOn: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      http20Enabled: true
    }
  }
}

// ---------------------------------------------------------------------------
// 3. Private Evidence Storage, Containers, Queue & Defender Malware Routing
// ---------------------------------------------------------------------------
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: storageAccountName
  location: appLocation
  kind: 'StorageV2'
  sku: {
    name: 'Standard_LRS'
  }
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
    publicNetworkAccess: 'Disabled'
    networkAcls: {
      bypass: 'AzureServices'
      defaultAction: 'Deny'
    }
  }
}

resource blobServices 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    isVersioningEnabled: true
  }
}

resource quarantineContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobServices
  name: 'quarantine'
  properties: {
    publicAccess: 'None'
  }
}

resource cleanContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobServices
  name: 'clean'
  properties: {
    publicAccess: 'None'
  }
}

resource deadLetterContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobServices
  name: 'eventgrid-deadletter'
  properties: {
    publicAccess: 'None'
  }
}

resource queueServices 'Microsoft.Storage/storageAccounts/queueServices@2023-05-01' = {
  parent: storageAccount
  name: 'default'
}

resource defenderQueue 'Microsoft.Storage/storageAccounts/queueServices/queues@2023-05-01' = {
  parent: queueServices
  name: 'defender-results'
}

resource malwareTopic 'Microsoft.EventGrid/topics@2023-12-15-preview' = {
  name: malwareTopicName
  location: appLocation
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    inputSchema: 'EventGridSchema'
    publicNetworkAccess: 'Enabled'
  }
}

// ---------------------------------------------------------------------------
// 4. Key Vault (RBAC Authorization, Private Access Only, No Inline Secrets)
// ---------------------------------------------------------------------------
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: appLocation
  properties: {
    tenantId: tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enableSoftDelete: true
    publicNetworkAccess: 'Disabled'
    networkAcls: {
      bypass: 'AzureServices'
      defaultAction: 'Deny'
    }
  }
}

// ---------------------------------------------------------------------------
// 5. AI & Document Intelligence Accounts (No Keys Output)
// ---------------------------------------------------------------------------
resource docIntel 'Microsoft.CognitiveServices/accounts@2023-05-01' = {
  name: docIntelAccountName
  location: appLocation
  kind: 'FormRecognizer'
  sku: {
    name: 'F0'
  }
  properties: {
    customSubDomainName: docIntelAccountName
    publicNetworkAccess: 'Enabled'
    disableLocalAuth: false
  }
}

resource aiAccount 'Microsoft.CognitiveServices/accounts@2023-05-01' = {
  name: aiAccountName
  location: aiLocation
  kind: 'AIServices'
  sku: {
    name: 'S0'
  }
  properties: {
    customSubDomainName: aiAccountName
    publicNetworkAccess: 'Enabled'
  }
}

// ---------------------------------------------------------------------------
// 6. Least-Privilege RBAC Role Definitions (Scoped to Containers/Queue/Vault)
// ---------------------------------------------------------------------------
var keyVaultSecretsUserRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
var storageBlobDataContributorRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
var storageQueueDataMessageProcessorRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '8a0f0c08-91a1-4084-bc3d-661d67233fed')

resource kvRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, apiApp.name, keyVaultSecretsUserRole)
  scope: keyVault
  properties: {
    roleDefinitionId: keyVaultSecretsUserRole
    principalId: apiApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource quarantineBlobRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(quarantineContainer.id, apiApp.name, storageBlobDataContributorRole)
  scope: quarantineContainer
  properties: {
    roleDefinitionId: storageBlobDataContributorRole
    principalId: apiApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource cleanBlobRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(cleanContainer.id, apiApp.name, storageBlobDataContributorRole)
  scope: cleanContainer
  properties: {
    roleDefinitionId: storageBlobDataContributorRole
    principalId: apiApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource defenderQueueRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(defenderQueue.id, apiApp.name, storageQueueDataMessageProcessorRole)
  scope: defenderQueue
  properties: {
    roleDefinitionId: storageQueueDataMessageProcessorRole
    principalId: apiApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// ---------------------------------------------------------------------------
// Outputs (Non-Sensitive Identifiers Only)
// ---------------------------------------------------------------------------
output apiAppId string = apiApp.id
output webAppId string = webApp.id
output apiPrincipalId string = apiApp.identity.principalId
output storageBlobEndpoint string = storageAccount.properties.primaryEndpoints.blob
output defenderQueueEndpoint string = '${storageAccount.properties.primaryEndpoints.queue}defender-results'
output keyVaultUri string = keyVault.properties.vaultUri
output postgresServerId string = postgresServer.id
output appVnetId string = appVnet.id
output dbRegion string = dbLocation
