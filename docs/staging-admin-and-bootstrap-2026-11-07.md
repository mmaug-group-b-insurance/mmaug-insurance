# Administrator and bootstrap handoff - 7 November 2026

## Latest user decisions and verified changes

- Team website/API IP allowlisting was withdrawn because team egress addresses are incomplete. Both main sites now have default Allow and the Approved-team-egress rule has been removed, verified by CLI readback. Entra authentication and server-side authorization remain required before publishing claim functionality. Database, storage and vault public access were not changed. SCM remains internet-reachable and requires authenticated deployment; no restricted SCM configuration is claimed.
- Approved total staging budget is now USD 500/month. The reported USD 620.20/month database estimate still exceeds the entire budget by USD 120.20 before other services. No resize, HA change or budget resource was applied. Obtain a lower estimate (for example by pricing HA disabled and smaller supported compute) before accepting ongoing spend; no claim is made that any specific alternative fits USD 500.
- Created dedicated user-assigned deployment identity id-mmaug-github-stg, client ID 4a279a53-7c61-4d8d-874d-a058135f4b94, principal ID 863dba30-77e1-49b6-af0d-2d26f7804bc2. Federation trusts issuer https://token.actions.githubusercontent.com, audience api://AzureADTokenExchange, subject repo:mmaug-group-b-insurance/mmaug-insurance:environment:staging.
- Created GitHub environment staging with a custom branch policy allowing main only. Stored public AZURE_CLIENT_ID, AZURE_TENANT_ID and AZURE_SUBSCRIPTION_ID as environment variables. Existing dev environment/secrets were not changed. No deployment client secret exists for this new identity. No workflow was pushed or executed remotely.
- Backend DNS/TCP checks were run by the user from App Service SSH. This is a viable private execution context, but availability of psql, Azure CLI and an authorized bootstrap login still needs checking.

## Why Entra is open

The Azure subscription account can manage many Azure resources but Graph app metadata reads return Insufficient privileges. Subscription RBAC does not establish Entra application-management authority. Registration values known from earlier screenshots are not proof of effective current configuration.

Admin must confirm which registration is actually used by the SPA (reuse existing registration or separately agreed SPA), add the exact SPA redirect https://app-mmaug-web-stg-01-bpd6hug0b6drhwah.eastus-01.azurewebsites.net, confirm delegated scope/consent and CLAIMS_OFFICER/ADMIN definitions and assignments. Existing full scope is api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user/mmaug_insurance_app. Verify token version, issuer and audience from an actual access token without logging it. Frontend reads ID-token roles; API-only assignments on a separate registration do not populate SPA ID-token roles automatically. Keep existing consumers intact.

## Administrator action list

1. Run scripts/Grant-StagingBackendAccess.ps1 after -WhatIf review. This grants vault secret reading, two container-specific Blob grants and queue message processing to backend principal 7110205a-5600-4d19-bff1-de24182309d2. The current account's earlier role-assignment attempt was denied; these grants are not claimed complete.
2. Run scripts/Grant-StagingDeploymentAccess.ps1 after -WhatIf review. Website Contributor is scoped separately to the two Web Apps, not the subscription/resource group. This identity can deploy/configure these apps but does not get direct database, vault or storage data permissions.
3. Enable Defender on-upload scanning on stmmaugclaimsstg01 only. Subscription override ON, cap 1 GB/month, sensitive-data discovery OFF, result topic evgt-mmaug-malware-results-stg. Preserve existing queue/Event Grid subscription, delivery identity and private access. Earlier failure involved Microsoft.Security/datascanners/write on the subscription StorageDataScanner resource. Do not enable a subscription-wide paid Defender plan. Confirm actual enabled state and scan-result delivery.
4. Provide the Entra confirmations above. Broad tenant administration for the developer is not required if the app owner applies the changes.

## Database bootstrap in the existing private console

In backend App Service > Development Tools > SSH, first run command -v psql, command -v az and command -v python3. A TCP probe does not establish that those tools are installed. If psql/az is absent, use an administrator-controlled VNet-connected bootstrap environment or deliberately install the necessary clients there; do not expose private services to work around missing tools. Files in an App Service container may be ephemeral.

With psql installed, connect interactively as the PostgreSQL administrator fumeh:

```sh
psql "host=pg-mmaug-claims-stg-01.postgres.database.azure.com port=5432 dbname=postgres user=fumeh sslmode=verify-full" -W
```

Use the trusted operating-system CA bundle (or an administrator-provided current CA file via sslrootcert) if libpq needs an explicit certificate path. Do not weaken sslmode to bypass certificate errors. Type the administrator password only into the hidden prompt. Inspect \du and \l. If mmaug_app/mmaug_claims already exist, inspect ownership rather than running CREATE again. Otherwise run the reviewed commands in infra/staging/bootstrap-database.psql interactively. The application password is entered through \password, not embedded in commands. For initial staging, the app owns only its database and runs Flyway; it has no CREATEDB/CREATEROLE/server-admin rights. Never use the PostgreSQL admin login in the app's DB_USERNAME.

Reconnect as mmaug_app to mmaug_claims with verify-full and run SELECT current_user, current_database(); and SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid();. Successful DNS/TCP alone is not authentication/TLS acceptance.

## Secrets bootstrap

An authorized bootstrap USER needs temporary Key Vault Secrets Officer at this vault plus private network access. The backend runtime identity needs Secrets User only; do not grant it permanent secret-write permissions. Azure CLI can sign in with device code in a trusted private console if permitted by tenant policy; do not display/copy any token. Never run az login --identity expecting the backend's read-only identity to create secrets.

Create the four secrets through that authorized private context: db-password (the mmaug_app password), docintel-key, ai-key, pii-encryption-key. The prepared scripts/bootstrap_staging_secrets.py uses hidden confirmation prompts and Key Vault REST request bodies in memory. It requires an authorized human CLI login in the project tenant, refuses redirected input and existing secret names, never generates/replaces an encryption key, and prints only names/versions. Copy the reviewed script into the private console and run python3 bootstrap_staging_secrets.py after user sign-in. For partially populated vaults use --names with only the missing names. Syntax/hidden-input refusal can be tested locally; live writes have not been executed. Memory clearing is best effort, not guaranteed zeroization. Do not pass values via --value in typed commands, log them, paste them into chat, or use frontend VITE settings for secrets. Default CLI secret-set JSON includes the value, so --output none is mandatory if using CLI instead.

For pii-encryption-key preserve the exact existing 32 UTF-8 byte key if retaining encrypted data. Only generate a new key after explicitly confirming a fresh database/no retained encrypted PII. Record the non-secret key version for the pinned Key Vault reference; do not rotate it implicitly. Revoke the bootstrap user's temporary secret-write grant after setup. Backend reference resolution should be inspected by status only, without retrieving values.

## Deployment checkpoint

After administrator grants, review/merge the local staging-access-check.yml and run it from main. It uses the staging environment and OIDC, reads the two Web Apps, and makes no deployment. Success proves federation and target read access only; deployment-write permission and SCM connectivity need separate verification. The staging-package.yml workflow remains a separate test/package-only step. Publishing actual claims functionality still requires verified Entra and secret/database configuration; scanning acceptance remains required for cleared-document processing.
