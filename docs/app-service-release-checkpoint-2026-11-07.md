# App Service release checkpoint - 7 November 2026

Scope: internal synthetic claims staging. This checkpoint prepares release packages; it does not authorize a larger cloud budget or establish deployed acceptance.

Later decisions on the same day supersede the IP/budget items below: the user withdrew website/API IP restrictions and approved USD 500/month. See staging-admin-and-bootstrap-2026-11-07.md for current state, restored public ingress, OIDC setup and the still-over-budget database estimate.

## Evidence and open dependencies

- User reports connected/synchronized bidirectional VNet peering, PostgreSQL DNS linkage to East US, approved Key Vault endpoint 10.20.3.6, private DNS linkage, enabled backend routing and successful backend DNS/TCP probes for PostgreSQL, Key Vault, Blob and Queue. These are user-reported checks, not end-to-end database/secret/SDK acceptance.
- CLI on 7 November confirms PostgreSQL remains General Purpose Standard_D4ds_v5 with healthy ZoneRedundant HA. User reports a USD 620.20/month estimate. User subsequently confirmed team approval of a higher budget but has not yet supplied its amount; no unlimited spend approval is inferred. D2ds_v5 with HA is reportedly USD 326.74/month. Premium SSD v2/128 GiB limits prevent the original Burstable/32 GiB configuration on this server. Preserve the current server pending the revised budget amount; no resize/replacement was performed.
- Backend application setting names remain empty. User supplied approved egress 195.158.87.220. Main-site rules on both apps were changed and read back: allow 195.158.87.220/32 at priority 100, default Deny. SCM rules remain unchanged and require a restricted deployment runner/access arrangement. HTTP testing from the approved address remains pending.
- Backend role-assignment inventory returned no grants. Attempt to assign Key Vault Secrets User was denied with AuthorizationFailed / Microsoft.Authorization/roleAssignments/write; no subsequent grants were attempted. `scripts/Grant-StagingBackendAccess.ps1` supplies reviewed vault/container/queue scopes for an authorized administrator, with identity/subscription/tenant checks and -WhatIf support. No elevated account-wide role is requested.
- Entra app metadata inspection still returns insufficient privileges. The existing app ID is known, but staging SPA redirects, effective roles/assignments, consent and access-token audience are unverified.
- Defender status inspection at 2026-11-07T14:59:01Z still shows on-upload scanning disabled, cap -1 and no result topic. Existing Event Grid subscription and private storage endpoints do not establish live scanning. Administrator must enable account-only scanning with subscription override, 1 GB/month cap, sensitive-data discovery off and the existing result topic. Do not enable a subscription-wide paid plan.

## Prepared package workflow

`.github/workflows/staging-package.yml` is manual and restricted to main. It requires an administrator-confirmed SPA client ID and full API scope, runs backend integration tests and frontend audit/typecheck/tests/build, and produces app.jar, frontend.zip, a public configuration manifest and SHA256SUMS tied to the exact commit. It neither logs in to Azure nor deploys. No provider-paid tests are enabled. Frontend source maps are excluded from the deployment ZIP. No frontend product behavior or Java API is changed.

The frontend's VITE settings are compiled into its bundle. Adding VITE values to App Service after building cannot repair an incorrectly built bundle. The workflow supplies the tenant-specific authority, exact deployed origin and API base URL at build time and sets the development bypass false. Its input checks establish syntax only, not Entra acceptance.

## Configuration and deployment order

1. Resolve the database budget/host decision. If replacement is selected, estimate a new PostgreSQL 16 Burstable B1ms server using a supported storage option, HA disabled and smallest practical disk. Confirm regional availability and private networking before creating anything. Do not delete the existing server or assume it is empty.
2. Confirm team CIDRs and main-site restrictions on both apps, and explicitly choose a runner that can access restricted SCM endpoints. A GitHub-hosted runner does not automatically have access to either private dependencies or an IP-restricted SCM endpoint. Never temporarily open the database or vault to all networks.
3. Have the Entra owner register the exact frontend HTTPS origin as a SPA redirect and confirm full scope/consent. Verify actual access-token issuer/audience without logging the token. Confirm CLAIMS_OFFICER/ADMIN assignments and frontend ID-token role visibility; separate SPA/API registrations need deliberate role alignment because the current frontend reads roles from its ID token. No client secret belongs in frontend configuration.
4. Verify backend identity has vault secret-read access and the narrowly required Blob/Queue permissions. Event Grid's delivery identity is distinct from the backend's consumer identity. Test successful secret-reference resolution without reading values into logs. Preserve the existing encryption key for retained encrypted data; pin its version to avoid accidental automatic key changes.
5. Create mmaug_claims, an application/migration role strategy and required grants from a controlled VNet-connected execution context. Verify PostgreSQL authentication and TLS certificate validation, not only TCP. Flyway staging loads db/migration only; do not enable db/seed. Provision any necessary synthetic lookup fixtures in a controlled, reviewed step.
6. Complete Defender configuration and real scan-to-queue acceptance. No document may be parsed before a verified clean result. Uploads remaining quarantined are expected while scanning is disabled; do not substitute a stub scanner.
7. Copy `infra/staging/backend-appsettings.example.json` to an untracked secure working location and replace every REPLACE_WITH value. The JSON contains references only, never secret values. The database hostname intentionally remains unresolved pending budget decision; audience must not be inferred from a scope URI. Configure staging settings before starting the application. Lower initial pool sizes are staging tuning proposals that require load checks.
8. Review/merge the package workflow; manually build from main using confirmed public Entra inputs. Verify checksums, then use a dedicated GitHub OIDC identity scoped to the two Web Apps for deployment. Do not reuse a SPA secret for deployment. Configure the frontend's static serving/SPA fallback and test a direct nested route. Startup configuration must bind the App Service port. A package build is not a deployment.
9. Deploy backend and frontend from the same reviewed candidate. Validate real sign-in, role and case authorization negatives, exact CORS, Key Vault references, database migrations, scanned upload/OCR/summary/review/audit, restart persistence, and rollback/restore. Monitor resource usage, denied access, stuck processing, scan delivery and spend before team acceptance.

## References

- https://learn.microsoft.com/en-us/azure/app-service/deploy-github-actions
- https://learn.microsoft.com/en-us/azure/app-service/app-service-key-vault-references
- https://learn.microsoft.com/en-us/azure/app-service/configure-language-nodejs

## Local verification

Workflow YAML parsed successfully; manual/main-only trigger, full SHA action pins, disabled bypass, secret-reference-only fields and frontend/backend origin alignment checked. TypeScript passed, all 35 frontend tests in seven files passed, and the production build succeeded using the known existing registration/scope as compilation inputs only. This does not verify that registration's consent, role assignments or staging redirect. Existing build warnings concern Zod annotation stripping and a large JS chunk. The GitHub package workflow has not run remotely. No backend source changed and no backend suite was rerun for these configuration/documentation additions.

Azure mutations were limited to the user-authorized main-site IP rules on the two Web Apps. Role assignment was denied. No credential values were retrieved, database migrations run, paid provider calls made or applications deployed.
