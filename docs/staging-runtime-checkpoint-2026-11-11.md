# Azure staging runtime checkpoint - 11 November 2026

The backend build passes. Application startup and team sign-in are not accepted yet.

## Applied and verified

- GitHub run 42987997587 initially failed Azure login with AADSTS700213. Added federated credential github-staging-environment to id-mmaug-github-stg, preserving existing dev and branch credentials. It trusts issuer https://token.actions.githubusercontent.com, audience api://AzureADTokenExchange and the exact subject repo:mmaug-group-b-insurance@339813633/mmaug-insurance@1410884094:environment:staging. The failed deployment job was retried and Azure login passed.
- Backend runtime is Java 17, HTTPS-only, system-assigned identity 7110205a-5600-4d19-bff1-de24182309d2, with existing VNet integration and route-all enabled.
- Applied staging profile, authentication enabled, tenant-specific v2 issuer, API audience 09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f, frontend HTTPS CORS origin, existing private PostgreSQL host, intended application user mmaug_app, storage/queue endpoints and Key Vault references. These are expected configuration values; actual token issuance and database authentication remain unverified.
- The old mmaug-claims-openai-01 account no longer exists. The current fr-mmaug-insurance resource exposes https://fr-mmaug-insurance.openai.azure.com/ and has a successful mmaug-insurance-gpt-5.4-mini deployment. Updated the example settings and runtime accordingly. No paid model/OCR request was made.
- No encryption key was generated or replaced. PII_ENCRYPTION_KEY remains pending a confirmed secret version.

## Administrator dependencies

1. Run scripts/Grant-StagingBackendAccess.ps1 after its -WhatIf preview. The current human account remains denied Microsoft.Authorization/roleAssignments/write. No backend grants succeeded. The backend needs Key Vault Secrets User, container-scoped Blob Data Contributor on quarantine and clean, and Queue Data Message Processor on defender-results. Its db-password reference currently reports AccessToKeyVaultDenied.
2. Initialise mmaug_claims and dedicated mmaug_app through a trusted private-network console using infra/staging/bootstrap-database.psql. Server pg-mmaug-claims-stg-01 is Ready, public access Disabled, and its database list contains only postgres and Azure system databases. Verify TLS with sslmode=verify-full. Do not put the administrator login in application settings.
3. An authorised human with temporary Key Vault Secrets Officer and private connectivity must initialise db-password, docintel-key, ai-key and pii-encryption-key using scripts/bootstrap_staging_secrets.py. The vault management-plane secret inventory was empty. ai-key must belong to the current fr-mmaug-insurance account. Do not share credentials in chat or logs. Only create a new PII key after explicitly confirming no encrypted data must be retained. Pin its version in the app setting, using the existing example reference format.
4. Entra application metadata remains denied to the current account. Confirm SPA registration, exact staging redirect, delegated consent, v2 access tokens and officer assignments on the appropriate enterprise applications. GitHub deployment federation does not establish user sign-in.
5. Defender account-level protection is enabled but on-upload scanning remains disabled. Enable the previously agreed account-only scan configuration, 1 GB/month cap, sensitive-data discovery off, and existing malware-results topic. Validate actual result delivery before document processing.

## Acceptance still required

Finish the deployment run, verify Key Vault references report Resolved, and check liveness and readiness. Verify unauthenticated protected APIs are denied, real officer sign-in works, and the synthetic claim/upload/scan/OCR/summary/human-review path succeeds. Neither a successful JAR upload nor App Service state Running proves application readiness.
