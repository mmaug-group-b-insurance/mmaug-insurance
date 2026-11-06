# Entra and networking review - 2 November 2026

## User decisions and supplied configuration

- Do not deploy Azure VPN Gateway. This supersedes the gateway candidate in the 30 October proposal and calculator estimate. No gateway has been deployed by this work.
- User reports application `mmaug_insurance_app` has client ID `09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f`. This is configuration metadata, not a secret. Whether the same registration serves both SPA and API is not established; do not automatically populate both client IDs.
- Previously observed tenant ID is `71beefd7-29f4-4b9e-9cb2-e78b5dc851ad`; confirm the application's Overview shows this tenant before token tests.

## Screenshot evidence

1. The API permissions screen shows delegated Microsoft Graph `email`, `User.Read` and `User.Read.All`, each marked Granted for Imoh-Tenant. This establishes displayed Graph consent, not consent for the MMAUG custom API. `User.Read.All` grants broader directory profile access than ordinary sign-in needs; establish a feature requirement with the app owner before keeping it as an application dependency. No permission has been revoked or changed.
2. Add-a-scope screen shows scope name `mmaug_insurance_app`, Admins only consent and Enabled state. The Application ID URI is redacted and partly includes an `access_as_user` segment. Do not reconstruct the full scope from the application ID or the old example `access_as_user` scope.
3. Expose an API screen for `mmaug_insurance_app` shows a saved enabled scope with Admins only consent. The full URI remains redacted/truncated. Authorized client applications is empty. Preauthorization is optional; its absence does not by itself prove that a client cannot receive consent or access the API.

## Remaining verification

- Obtain the full Application ID URI and exact scope string using the Expose an API copy control. Those identifiers are not client secrets.
- Confirm which registration is the SPA and which is the resource API. Reuse the provided ID in the correct role only after this is established. A second registration may already exist; do not create duplicates.
- Confirm SPA platform configuration and exact local/staging redirect URI. The dev preview runs at `http://127.0.0.1:5174`; this is distinct from `http://localhost:5173`.
- Confirm the SPA has delegated permission to the MMAUG API scope and the required admin consent. Graph permissions do not replace that permission.
- Confirm API app-role values and user assignments, including `CLAIMS_OFFICER`, `ADMIN`, and optional approved roles. A scope is permission to call the API; an app role is not established by naming a scope.
- Confirm access-token version/audience/issuer, required delegated scope and roles through a real authenticated request. Do not use a Graph access token for the claims API. Do not paste raw tokens in chat.
- Keep the frontend development bypass off in authenticated acceptance tests; preserve frontend source and Spring Boot interfaces.

## VPN clarification and cost impact

The no-deployment instruction is binding. The physical-firewall explanation applies to a conventional site-to-site connection to an on-premises network. Point-to-site VPN connects an individual computer using VPN client software and does not require that on-premises appliance. This clarification does not authorise deploying either mode.

The historical calculator estimate was USD 197.37/month. Excluding its gateway (USD 153.30) and the Standard IP explicitly labelled for that gateway (USD 3.65) leaves approximately USD 40.42 in the other listed components. This arithmetic is not a revised complete private-staging estimate: replacement access, DNS and network costs remain unresolved. The original export is retained as historical evidence. Do not silently replace private network access with a public endpoint.

## Sources

- [Point-to-site VPN](https://learn.microsoft.com/en-us/azure/vpn-gateway/point-to-site-about)
- [Site-to-site device requirement](https://learn.microsoft.com/en-in/azure/vpn-gateway/add-remove-site-to-site-connections)
- [Expose a web API and configure client permission](https://learn.microsoft.com/en-us/azure/active-directory/develop/quickstart-configure-app-expose-web-apis)
- [Scopes and optional preauthorization](https://learn.microsoft.com/en-us/entra/identity-platform/scopes-oidc)

## Full identifiers supplied by user

- Application/client ID: `09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f`.
- Tenant ID: `71beefd7-29f4-4b9e-9cb2-e78b5dc851ad`.
- Application ID URI: `api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user`.
- Enabled, admin-consent-only full scope: `api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user/mmaug_insurance_app`.
- SPA redirect URIs: none configured.
- App role definitions: none configured, including CLAIMS_OFFICER.

This supersedes the earlier missing/redacted URI entries. Preserve the supplied full scope exactly; its delegated scope value is `mmaug_insurance_app`. The `access_as_user` portion belongs to the Application ID URI in this registration. Do not silently rename either.

Next configuration work: confirm SPA registration choice; register its exact redirect URI as SPA (current preview `http://127.0.0.1:5174`); define user-facing API roles such as CLAIMS_OFFICER and ADMIN according to the agreed role model, assign the officer test account, configure the custom API permission and consent on the SPA, then verify an actual access token and authenticated API call. Determine the expected audience from the API token version and validate it in Spring Boot; do not assume the full scope string is the audience. No portal configuration or frontend source was changed by recording these values.

## Separate-registration implementation plan

The recommended SPA/API separation and reviewable administrator change set are documented in [entra-setup.md](entra-setup.md). The existing UI reads SPA ID-token roles, requiring matching UI role definitions/assignments while the backend independently enforces API roles. Current portal access lists the user as an owner but disables Create app role. The local backend now validates the configured API audience; no Entra configuration has been changed.
