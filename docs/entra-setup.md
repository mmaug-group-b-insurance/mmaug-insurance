# Entra setup for separate SPA and API registrations

Decision recommendation: preserve the existing API registration and use a separate single-tenant SPA registration. No existing app is renamed. No VPN gateway is deployed. This plan is prepared locally; no Entra write has been made.

## Verified access and integration constraints

The Chrome portal currently lists the user as an API application owner, but Create app role is disabled with a permissions notice. The API has no app roles. Azure CLI is installed but is not authenticated. App ownership alone has not provided working configuration access through this portal session. An authorised tenant administrator must apply the remaining directory changes or resolve the restricted administrative access. Do not grant broad tenant administration as a workaround without the tenant administrator's review.

The existing frontend's `getUserRoles` reads roles from the SPA ID token. Roles on the API alone appear in the API access token, not automatically in the SPA ID token. Preserve frontend source by defining matching user-facing role values on both registrations and maintaining the officer assignment on both enterprise applications. The backend remains the authority for API access; client role visibility cannot grant server permission. Longer-term elimination of duplicated UI assignments is separate frontend work.

## Administrator change set

1. Keep the resource API registration `mmaug_insurance_app`, client ID `09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f`, tenant `71beefd7-29f4-4b9e-9cb2-e78b5dc851ad`.
2. Preserve its existing Application ID URI `api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user` and enabled delegated scope `mmaug_insurance_app`. The complete requested scope is `api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user/mmaug_insurance_app`. Preserve admin-only consent. Coordinate the API access-token version 2 setting with existing consumers before changing it.
3. Find or create `mmaug_insurance_spa`, Accounts in this organizational directory only. Record its newly assigned client ID. Configure platform Single-page application and redirect `http://localhost:5174`. Use this same localhost origin when testing authenticated login. The earlier preview URL used 127.0.0.1; Microsoft currently requires a manifest edit for an HTTP 127.0.0.1 redirect, so localhost avoids that portal limitation. Add an eventual HTTPS staging URL only after it is known. Leave implicit grants off; use authorization code with PKCE through MSAL. Do not create a SPA client secret.
4. In the SPA's API permissions, add the existing MMAUG API's delegated `mmaug_insurance_app` scope, then have an authorised administrator grant the required consent. Do not request User.Read.All for this sign-in path; do not revoke existing grants on the API without separately reviewing their use. Do not invent a scope GUID: select the actual published permission.
5. Define enabled `CLAIMS_OFFICER` and `ADMIN` roles, allowed members Users/Groups, on the API and matching display roles on the SPA. Do not enable application-only role membership for these human roles. Do not add CLAIMANT, AUDITOR or other optional scope as part of this change.
6. In each enterprise application, assign the officer test account `fumeh34@gmail.com` to CLAIMS_OFFICER. Do not assign ADMIN by default. Have the tenant administrator handle consent and assignment; registration ownership is distinct from enterprise-application assignment authority.
7. Verify with fresh sign-in after configuration. Old cached tokens may lack new role claims. Do not paste tokens or secrets in chat.

The reviewable change plan is `infra/staging/entra/registration-change-plan.json`. It is not an executable PATCH. Merge additions against live arrays; preserve existing IDs/roles/scopes. Deterministic proposed role IDs are included only for the proposed new roles. No supplied client ID has been reused as a guessed SPA ID.

## Local settings after the SPA ID is available

Set the following in the existing frontend's runtime/build environment without modifying frontend source:

```dotenv
VITE_API_BASE_URL=http://localhost:5174/api/v1
VITE_ENTRA_CLIENT_ID=<actual-new-SPA-client-ID>
VITE_ENTRA_TENANT_ID=71beefd7-29f4-4b9e-9cb2-e78b5dc851ad
VITE_ENTRA_AUTHORITY=https://login.microsoftonline.com/71beefd7-29f4-4b9e-9cb2-e78b5dc851ad
VITE_ENTRA_REDIRECT_URI=http://localhost:5174
VITE_ENTRA_API_SCOPE=api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user/mmaug_insurance_app
VITE_DEV_BYPASS_AUTH=false
```

The API base above assumes the temporary local preview proxy on 5174 is running. It is not the deployed API URL. The preview proxy currently sets an allowed development Origin for the local API; final staging CORS must use the agreed actual origin. Current preview processes may be stopped; configuring values is not proof that services are running.

For the backend, enable authentication and use:

```dotenv
APP_SECURITY_ENABLED=true
ENTRA_ISSUER_URI=https://login.microsoftonline.com/71beefd7-29f4-4b9e-9cb2-e78b5dc851ad/v2.0
ENTRA_AUDIENCE=09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f
```

These issuer/audience settings require API v2 access tokens. V2 `aud` is the API client ID, not the SPA client ID or full scope URI. For a local-profile run, explicitly override app.security.enabled as its profile file currently disables security by default. API calls without authentication must be denied. Shared staging must use the staging profile and real service providers, not local stubs.

## Acceptance evidence

- API decoder retains normal signature, issuer and time validation and now also checks the configured audience. Tests reject Graph, SPA, full-scope and missing audiences; real token issuance has not been tested.
- Verify the API token has expected issuer/tenant, API audience, `scp` containing `mmaug_insurance_app`, and the officer role. The backend still needs explicit required-scope enforcement in addition to role checks.
- Verify the SPA ID token has its own audience and officer display role. Never send an ID token to the claims API.
- Verify officer sign-in, missing-scope rejection, missing-role rejection and assigned-case access. Role definitions alone do not establish per-case isolation.
- Verify logout and exact redirect round trip; confirm the dev bypass is off in both layers. No test should claim real sign-in until the SPA exists and the authorised administrator has configured consent and assignments.

Sources: [SPA registration](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-single-page-app-sign-in), [Token audience validation](https://learn.microsoft.com/en-us/entra/identity-platform/claims-validation).

Redirect reference: [Microsoft redirect URI limitations](https://learn.microsoft.com/en-us/entra/identity-platform/reply-url). The switch to localhost affects environment configuration and the URL used for testing, not frontend source.

Validation on 2 November 2026: 39 non-Docker tests passed with zero failures/errors/skips, including five new audience-validator tests. The full 42-test run had three application-context errors because Docker was unavailable; Docker Desktop was started but reported an engine startup error. PostgreSQL integration and actual Entra sign-in have not passed in this session. Frontend source diff is empty; git diff --check passed. Changes remain local and uncommitted.
