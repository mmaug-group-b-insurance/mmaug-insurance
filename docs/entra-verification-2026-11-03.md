# Entra verification — 3 November 2026

Read-only verification in Azure Portal, signed in as fumeh34@gmail.com in Imoh-Tenant (71beefd7-29f4-4b9e-9cb2-e78b5dc851ad).

- Existing API application: mmaug_insurance_app, client ID 09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f.
- Application ID URI remains api://09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f/access_as_user.
- A SPA redirect now exists on this registration: http://localhost:5174.
- App roles page reports no app roles. Create app role is disabled, with a permissions warning.
- Authentication page: Add Redirect URI disabled, with a permissions warning.
- Owned applications lists only the existing API registration.
- All applications reports Insufficient privileges to view applications. A separate SPA registration cannot be confirmed or ruled out from this account.
- No Entra configuration or cloud resource changes made during verification.

Next dependency: administrator confirms effective app-management permissions for this guest account, or completes the separate SPA/API configuration in docs/entra-setup.md and supplies the SPA client ID. Do not create a duplicate frontend registration without checking existing registrations.
