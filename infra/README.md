# Infrastructure as Code (`infra/`)

This directory contains the declarative Azure Bicep definition (`infra/staging/main.bicep` and `infra/staging/main.bicepparam`), environment configuration templates, and read-only observed inventories for the **MMAUG Insurance Claims Agent** staging footprint in resource group `rg_mmaug_insurance`.

## Declarative Topology (`infra/staging/main.bicep`)

- **Compute (`eastus`):**
  - `asp-mmaug-stg` (`Microsoft.Web/serverFarms`, Linux `B1`)
  - `app-mmaug-api-stg-01` (`Microsoft.Web/sites`, Java 17, system-assigned managed identity, HTTPS-only, VNet route-all enabled, `/actuator/health/readiness` probe)
  - `app-mmaug-web-stg-01` (`Microsoft.Web/sites`, React SPA static/Node host, HTTPS-only)
  - `id-mmaug-github-stg` (`Microsoft.ManagedIdentity/userAssignedIdentities` for GitHub Actions OIDC deployment federation)
- **Private Evidence Storage & Malware Routing (`eastus`):**
  - `stmmaugclaimsstg01` (`Microsoft.Storage/storageAccounts`, public access disabled, shared key disabled, blob versioning enabled)
  - Blob containers: `quarantine`, `clean`, `eventgrid-deadletter`
  - Storage queue: `defender-results`
  - Event Grid topic: `evgt-mmaug-malware-results-stg`
- **Secret Management (`eastus`):**
  - `kv-mmaug-claims-stg-01` (`Microsoft.KeyVault/vaults`, RBAC authorization, soft-delete enabled, public access disabled, no inline secret values in IaC)
- **AI & OCR Providers:**
  - `mmaug-claims-docintel-01` (`Microsoft.CognitiveServices/accounts`, `FormRecognizer` `F0` in `eastus`)
  - `fr-mmaug-insurance` (`Microsoft.CognitiveServices/accounts`, `AIServices` `S0` in `swedencentral`, hosting `mmaug-insurance-gpt-5.4-mini`)
- **Stateful Data & Network References (Existing Resources — Never Overwritten):**
  - `pg-mmaug-claims-stg-01` (`Microsoft.DBforPostgreSQL/flexibleServers` in `centralus`, referenced via `existing` keyword so declarative deployments never recreate or drop retained encrypted data)
  - `vnet-mmaug-claims-stg-eus` (`Microsoft.Network/virtualNetworks` in `eastus`, peered to `rg_mmaug_insurance-vnet` in `centralus`)
- **Least-Privilege RBAC Role Assignments:**
  - `Key Vault Secrets User` scoped to `kv-mmaug-claims-stg-01`
  - `Storage Blob Data Contributor` scoped separately to `quarantine` and `clean` containers
  - `Storage Queue Data Message Processor` scoped to `defender-results` queue

## Validation Commands

Compile and validate the Bicep template offline without mutating Azure resources:

```powershell
az bicep build --file infra/staging/main.bicep
az bicep build-params --file infra/staging/main.bicepparam
```

Preview against the live resource group (requires Reader/Contributor + RBAC read):

```powershell
az deployment group what-if `
  --subscription 9a2ab22e-5269-4da3-a16a-eeed2b0c09aa `
  --resource-group rg_mmaug_insurance `
  --template-file infra/staging/main.bicep `
  --parameters infra/staging/main.bicepparam
```

## External Dependencies Outside Group-Scoped Bicep

1. **Microsoft Entra ID Application Registration (`09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f`):** Tenant directory object requiring Entra application administrator privileges (`spa.redirectUris`, `scp=mmaug_insurance_app`, app roles `CLAIMS_OFFICER`, `AUDITOR`, `ADMIN`).Tracked in `infra/staging/entra/registration-change-plan.json`.
2. **Subscription-Scoped Defender for Storage Scanner (`Microsoft.Security/pricings` & `datascanners`):** Requires subscription Security Admin (`capGBPerMonth=1`, `isEnabled=true` on `stmmaugclaimsstg01` only).
3. **Key Vault Secret Values & Retained Database:** Secret values (`db-password`, `docintel-key`, `ai-key`, and the preserved 32-byte `pii-encryption-key`) are populated interactively via `scripts/bootstrap_staging_secrets.py` and never stored in source control.
