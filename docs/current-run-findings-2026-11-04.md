# Current-run findings and review acceptance — 4 November 2026

## Verified local acceptance

Claim CLM-2026-000004, summary v4 (`801b1420-83d9-4073-a0c8-1ca83ddbf56b`), analysis run `aae2a0f9-5f50-469d-bd9c-9ebf15f05146`.

The user submitted ACKNOWLEDGED review `06d5b1a4-b277-46e8-9985-88eac5f9b90c` at 2026-11-04T14:46:47.334227Z. The stored review references the exact v4 summary and local synthetic reviewer identity. It records follow-up remains required. No claim approval or discrepancy resolution was submitted by the agent.

## Corrected behaviour

The claim had twelve stored findings: three discrepancies reproduced across four analysis runs. The default Findings endpoint previously returned all runs without identifying the run in its DTO.

- GET `/api/v1/claims/{claimId}/findings` now selects the claim's latest completed-analysis pointer (`latestRunId`). A failed attempt does not advance this existing pointer, so the prior completed run remains visible.
- An explicit `runId` selects a historical run only after verifying it belongs to the claim. Unknown runs return 404, cross-claim runs 403, combining runId and includeHistory 422.
- `includeHistory=true` retains access to every historical and pre-run finding. Nothing is deleted, silently resolved or rewritten.
- Each finding includes additive `runId` provenance. Existing type/severity/resolved filters apply within the selected scope.
- Before the first completed run, the default list selects only unbound pre-run findings. Afterward those remain available through history.
- Dashboard open-findings counts use the same current-run scope. The frontend source is unchanged.

This intentionally changes the default API selection from all history to current-run findings. Consumers needing all history must opt in. Existing officer resolutions remain specific to their finding/run and are not automatically copied to regenerated findings.

## Validation

Isolated PostgreSQL integration fixtures cover current, historical, resolved and unbound findings, cross-claim/unknown run rejection, invalid query combinations and dashboard count agreement. The workflow suite also checks versioned acknowledgement and immutable machine statements. Paid live OCR/model tests remain disabled for automated checks.

Local acceptance after restarting the backend: the claim's Findings tab should show three current discrepancies; includeHistory=true should still return twelve. Review history should retain the user's v4 acknowledgement.

## Findings evidence contract correction

The local Findings screen exposed a separate response mismatch: evidence was a list of strings, while the existing frontend EvidenceDrawer requires objects with sourceType. This could crash rendering even when the API returned three findings successfully.

The backend now maps stored provenance to typed document/tool references using the existing evidence DTO. Document/extraction identity is checked against the finding claim; tool references match recorded envelopes for the finding run. Raw strings remain available as evidenceRefs, including unrecognised references. No frontend source or stored findings were changed.

The isolated HTTP integration check now asserts document sourceType/name, tool sourceType/call ID, and preservation of unknown raw references. Full suite: 99 run, 97 passed, 2 opt-in live tests skipped; no failures/errors. Log: C:\Users\fumeh\.cache\mmaug-preview\findings-evidence-contract-tests.log.

This is verified in isolated integration tests. The already-running preview must restart from the user's credential-bearing PowerShell session before browser acceptance; no new analysis is required.

## Live browser/API acceptance completed

After restart, the user confirmed Findings rendered with document/tool citations. The user resolved POLICY_INACTIVE_ON_DATE and VEHICLE_MISMATCH on run aae2a0f9-5f50-469d-bd9c-9ebf15f05146. Read-only API verification confirmed both saved notes/timestamps, one unresolved policy-number CONTRADICTION, and dashboard openFindingsCount=1. The v4 acknowledgement remains retained. No extra analysis, officer review, resolution or consequential claim action was submitted by the agent during this verification.
