# API contract regression increment — 4 November 2026

Backlog: #75 (OpenAPI contract checks), supporting #74 and FR-11/12 review/audit acceptance. Existing issue bodies were read through the authenticated local GitHub CLI; no issue was edited or closed.

## Implemented

`src/test/resources/contracts/openapi-baseline.json` captures the currently served OpenAPI 3.1 document after the Findings evidence correction. The Spring/HTTP integration suite compares generated operations, requests, responses, components, security declarations and metadata against that baseline. Only the deployment-specific root servers entry is excluded. A shape change requires an explicit baseline update that reviewers can inspect.

The test also asserts that FindingResponse.evidence items reference EvidenceRefResponse. Existing integration fixtures check actual document and tool evidence objects, raw provenance retention, current/history run selection, dashboard count agreement and version-specific review. Snapshot agreement alone does not establish implementation correctness or approve every existing API choice. Map/Object schemas on agent responses still need stronger typing to improve static coverage; runtime shape checks remain necessary.

Verification: **100 tests run, 98 passed, 0 failures/errors, 2 opt-in live-provider tests skipped**. Log: `C:\Users\fumeh\.cache\mmaug-preview\openapi-contract-tests.log`.

## Updating intentionally

Generate the OpenAPI document from the changed backend, remove only the root servers entry, format JSON with two-space indentation, and inspect the diff with the affected consumer owner. Do not auto-accept baseline changes merely to make CI green. Run scripts/Invoke-GroundedSprintChecks.ps1 against isolated synthetic test databases. It disables paid live provider tests.

## Live acceptance observed

CLM-2026-000004 has summary v4 with the user's exact-version acknowledgement retained. Findings now displays typed document/tool evidence. POLICY_INACTIVE_ON_DATE and VEHICLE_MISMATCH were resolved by the user with test notes; the policy-number CONTRADICTION remains unresolved. The API dashboard openFindingsCount is 1. These notes demonstrate the mechanics of synthetic review, not a business determination that the evidence discrepancies are settled.

## Next priority

#35 and #82: make processing durable across transaction rollback and process interruption. The current synchronous orchestrator holds a claim database lock while OCR/model calls run. Design the run reservation/active-run uniqueness, transaction boundaries, attempt budget and abandoned-run recovery together; verify concurrent start, database rollback, provider timeout and restart recovery. Preserve immutable summary history, existing Java interfaces and the unchanged frontend. Do not close these issues based only on today's happy-path acceptance.

Private staging still requires the outstanding identity/access, scanner/private-storage, network/cost and recovery acceptance gates. No deployment or new paid resource is part of this increment.


Later processing-reservation/recovery work and its expanded 107-test suite are recorded in [durable analysis sprint](durable-analysis-sprint-2026-11-04.md). Its optional Idempotency-Key request header is an intentional additive baseline change.
