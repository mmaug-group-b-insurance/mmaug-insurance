# Azure OCR integration — 4 November 2026

Implements the OCR portion of extraction work (#30/#68), preserving DocumentIntelligenceClientWrapper and all frontend contracts.

## Enable

Set backend-only environment variables DOCINTEL_PROVIDER=azure, DOCINTEL_ENDPOINT=https://<resource>.cognitiveservices.azure.com and DOCINTEL_KEY through the local process environment or deployment secret store. Never use a VITE variable for the key. The resource endpoint must be HTTPS with no path/query. No resource is created by this change.

The adapter calls prebuilt-layout, REST API 2024-11-30. Keep app.docintel.model=prebuilt-layout so persisted extractor metadata matches the adapter. Supports PDF, PNG, JPEG and TIFF, at most 15 MiB. Sends binary content only after the existing CLEAN scan gate. Polls at one-second intervals for up to 45 seconds, with 10-second request timeouts; no automatic resubmission of billable analysis. Rate limiting and denial produce explicit failure codes. Redirects are disabled, operation URLs must match the configured origin and expected result path. Provider error bodies are not returned to users or logged by the adapter.

Page lines are separated by form feeds, preserving blank pages for the existing field provenance parser. Empty text, malformed page numbering, provider failure and timeout do not become successful evidence.

## Verification and limitations

Mocked HTTP contract tests cover page boundaries including a blank first page, credential-forwarding rejection, sanitized access denial and invalid endpoint configuration. Existing extraction/structuring tests run alongside these.

Live Azure OCR has NOT been verified: a configured resource endpoint, backend credential and approved synthetic test upload are still needed. This adapter is implemented, not deployed or accepted as production-ready. It does not resolve the existing no-op malware scanner, private Azure storage, model synthesis, case authorization or Entra staging dependencies. Local/dev/test retain the explicit stub when DOCINTEL_PROVIDER is unset. No frontend files or cloud resources changed.

Reference: https://learn.microsoft.com/en-us/rest/api/aiservices/document-models/analyze-document?view=rest-aiservices-v4.0%20(2024-11-30)

## Opt-in live smoke test

AzureOcrLiveTest generates a PNG containing only synthetic policy SYN-48271 and vehicle TEST-123 in memory, and checks the actual Azure response. It does not upload any existing claim files or bypass the application scan gate; it tests the adapter independently. It is skipped unless MMAUG_RUN_LIVE_OCR=true. Running it submits one potentially billable analysis to the configured Azure resource.

In a PowerShell session with JDK 17 and securely configured DOCINTEL_ENDPOINT and DOCINTEL_KEY, set MMAUG_RUN_LIVE_OCR=true and run the Maven wrapper from mmaug-claims-agent-backend with: .\mvnw.cmd '-Dtest=AzureOcrLiveTest' test. Unset the opt-in flag afterwards. Never commit credentials. A passing result proves only OCR connectivity and this synthetic example, not full application acceptance.

4 November follow-up: local process has no DOCINTEL configuration; Azure CLI requires sign-in. Browser automation failed before resource inspection, so resource existence remains unverified. No paid resource or live OCR request was created during this check.

## Application flow follow-up

User reported a successful live Azure smoke test on 4 November 2026 against https://mmaug-claims-docintel-01.cognitiveservices.azure.com. This is user-reported evidence; the agent did not obtain the key or independently rerun the live test.

Fixed local profile masking DOCINTEL_ENDPOINT. Extraction GET/POST now check DocumentService access before touching extraction results or OCR; triggering extraction additionally requires CLAIMS_OFFICER or ADMIN. Existing broad officer access remains unchanged, so this does not establish case-assignment authorization.

Structuring captures labelled policy numbers without a synthetic-only prefix and recognizes Vehicle Registration. Policy validity remains a separate validation/tool concern. Tests exercise actual upload, validation, scan clearance, extraction, structuring, raw evidence storage and reopened results with simulated repositories/storage/OCR; they also reject denied access and auditor-triggered extraction. These tests do not prove PostgreSQL durability or live full-stack behavior.

Start-AzureOcrPreview.ps1 starts a loopback-only local backend using process credentials. Supply an isolated synthetic DB URL and username and set DB_PASSWORD via a hidden prompt, alongside DOCINTEL_KEY. It does not create or migrate a shared/production database intentionally; ensure the supplied database is disposable preview data because normal Flyway startup runs migrations/seeds. Local auth and no-op scanner remain development-only. Do not deploy this preview configuration.

The live application acceptance step remains: start the backend with these settings, upload a synthetic supported document to an existing synthetic claim, trigger POST /api/v1/documents/{id}/extraction, GET the same extraction and reopen the claim. Verify the original document ID, policy/registration values, page/snippet provenance and EXTRACTED state. A failed OCR must show FAILED without fabricated fields. No live full-application run was performed in this follow-up because the successful user PowerShell credentials are not inherited by the agent session.
