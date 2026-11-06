# Evidence summary and review sprint — 4 November 2026

## Sprint outcome

Implemented the source-linked deterministic summary/review increment using the existing Java/Spring Boot backend and unchanged frontend contract. It now constructs constrained source quotations from extracted document fields and allowlisted policy/coverage/history snapshot values. This is a working grounded workflow foundation, not a claim that an external LLM has been integrated or that the application is production-ready.

## Backlog alignment

| Existing issue | Work delivered | Acceptance status |
| --- | --- | --- |
| #33 US-07 grounded summary and citations | Saved statement citations, exact extraction IDs, tool snapshot references, same-claim/run checks, supported-template verification, conflict preservation and explicit unknowns | Implemented and tested; current preview/UI restart acceptance pending |
| #34 US-08 version-specific acknowledgement | Cross-claim/version statement rejection, stale review rejection, annotations preserve original machine text, latest-version review and analysis serialized on the same claim lock | Real PostgreSQL/HTTP review tests passed |
| #35 truthful processing/recovery | Verified-source step records actual support checks; unsupported statements excluded; all-unsupported draft fails; expected OCR outage/quarantine failures persist without fake summary/tool results | Expected failure paths tested; unexpected database transaction failures and durable asynchronous recovery still open |
| #74 claim workflow integration | Isolated full Spring context, HTTP security, actual Flyway/PostgreSQL/storage/summary/review flow | Four workflow integration scenarios passed |
| #75 API contract checks | Existing frontend summary/evidence/run shape exercised; extractionId is additive | Focused contract coverage only; not full OpenAPI conformance |
| #69 real LLM summary generation | Not implemented in this sprint; no model resource or credential configured | Remains open |

No issues closed, comments posted, commits pushed or PRs created by this sprint.

## Source and review behavior

- GroundedSummaryBuilder uses the current run's selected successful extractions and tool snapshots. Each document statement preserves document ID, extraction ID, page, snippet and field name; tool facts preserve toolCallId. It keeps competing document values as separate UNCERTAIN statements. Failed/instruction-flagged/unsupported sources cannot produce factual statements.
- EvidenceVerifier loads the persisted source and checks exact value/page/snippet, source scope, clean/nondeleted state, policy identity, successful tool outcome and the constrained statement template. A real citation ID attached to an invented assertion fails. This verifies support against stored snapshots; it does not prove source truth, OCR correctness or semantic entailment of arbitrary prose.
- Retrieved facts are limited to recorded policy number/status/period and returned coverage/history counts. Counts are not eligibility, coverage interpretation, fraud or liability judgments. Five-year history lookup remains a demonstration default.
- New summary overall confidence remains conservatively LOW; document field confidence remains an uncalibrated parser heuristic. LLM metadata is grounded-template-v2, explicitly deterministic. Missing facts and pending rule/coverage interpretation are listed in uncertainty notes.
- Summary latest/version endpoints enforce existing ClaimService access rules. Those rules still allow officer-wide access; assignment-specific authorization and real Entra mapping remain separate work.
- Reviewer annotations are stored in officer_edit with original text, correction, reason, reviewer and exact summary association. Machine statements and older summaries are preserved. Stale summary review returns a validation error; forged cross-claim or cross-version references are denied before any review is saved. Acknowledgement cannot approve/reject/pay a claim or mutate policies.
- Summary uncertainty metadata now parses JSON correctly, including commas/quotes. Evidence responses expose document/tool names through the existing UI fields.
- InternalToolsController now has a read-only transaction around DTO mapping, fixing the lazy customer access defect observed in its HTTP policy endpoint. Integration GET succeeded under SERVICE_AGENT authority.

## Migration and operation

V14__pin_summary_evidence_to_extraction.sql adds nullable extraction_id with a foreign key/index to evidence_ref. Existing historical summaries remain readable and keep their original disclaimers; the migration does not manufacture citations for older summaries. New summaries use exact extraction provenance. No original documents or preview records were removed.

The orchestrator, document upload and review serialize per claim through the existing pessimistic row-lock query. This protects version selection, but long synchronous processing holds the lock and is not a scalable queue/worker design. Lock timeout tuning, transaction boundaries and independent durable failure recovery remain follow-ups.

Restart the existing backend once using scripts/Start-AzureOcrPreview.ps1 -UseExistingSyntheticDatabase, after Ctrl+C in its actual running window. Flyway applies V14. Generate a new analysis of the already-extracted synthetic CLM-2026-000004 to inspect the new citation chips and review annotations; the earlier summary is retained. Reprocessing is not required merely to inspect an older version. Frontend source is unchanged and need not be rebuilt for the additive citation metadata.

## Verification evidence

Full backend Maven test suite: 86 tests run, 85 passed, zero failures/errors, one skipped (AzureOcrLiveTest deliberately opt-in). Ran 4 November 2026 with JDK 17 and two uniquely named temporary PostgreSQL 18 databases on local port 55432. The preview database was not migrated or changed by the test run. Both temporary databases were removed afterward. CI retains PostgreSQL 16 Testcontainers defaults; that Docker-specific execution was not repeated locally.

GroundedWorkflowIntegrationTest covers:

1. Authenticated HTTP generic-MIME upload, duplicate rejection, actual storage/repository writes, mocked OCR, read-only tools, persisted citations, source-linked summary retrieval, a second immutable version, stale/cross-claim review denial, correction without machine-output mutation, unauthenticated/unauthorized summary denial, and the internal policy HTTP DTO path.
2. OCR outage persists a failed run/extraction and does not create a summary or fabricated tool calls.
3. Quarantined evidence never reaches OCR and cannot produce a summary.
4. Missing policy returns NOT_FOUND for all three tools, preserves sourced document facts, and does not invent policy/history summary statements.

Unit/contract regressions additionally cover unsupported prose with a real citation, wrong page, cross-claim document, cross-run tool snapshot, deleted/unscanned/injection evidence, missing citations, conflicting values, immutable annotation, stale/cross-version edits, MIME/duplicates, audience validation and the existing guardrails.

Repeat locally with scripts/Invoke-GroundedSprintChecks.ps1. It creates only randomly named mmaug_sprint_test_<uuid> databases, rejects other native test database URLs, disables live OCR, and restores process environment settings. CI can use ./mvnw verify with Docker available. Current local run proves simulated-provider integration and source-supported persistence; new live Azure/browser acceptance of this increment remains pending until the preview backend restarts.

## Remaining release work

Real LLM provider (#69), authenticated Entra staging, actual scanner/private Azure storage, business-rule retrieval and coverage interpretation, calibrated evaluation dataset/targets, durable failure/recovery boundaries, concurrency/load tests beyond current checks, full OpenAPI validation, and Azure deployment/cost/network acceptance remain outside this completed increment. Existing USD 100/month synthetic staging target and no-VPN-Gateway instruction are unchanged.

## Live preview acceptance after restart

4 November 2026: preview Flyway V14 confirmed applied. Generated run 77fdaade-f1f8-41ab-92e7-a131e9c5282f for already-extracted CLM-2026-000004; no additional OCR was needed. Subsequent summary GET returned version 2, grounded-template-v2, 11 statements with zero missing citations: five DOCUMENT references and six TOOL_RESULT references. VERIFY_EVIDENCE step SUCCESS records support for all 11 constrained statements. Run remains ESCALATED for human review. Version 1 retains original summary ID 2dd7ddab-4f43-4eff-b7c6-671bfa5e47ac and its original deterministic-v1 output. Browser rendering of the new version still requires refreshing the user's current summary page.
