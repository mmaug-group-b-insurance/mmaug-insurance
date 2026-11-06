# Provider transaction separation sprint — 5 November 2026

Backlog: #35 truthful recovery, #82 transaction boundaries, #74 workflow acceptance and #75 response contracts. BRD NFR-04/06/09 and FR-11/12. This increment preserves the Java/Spring Boot implementation and frontend source.

## Delivered

The analysis path no longer holds a database transaction or claim lock while OCR or the model is running. AgentExecutionService retains its no-ambient-transaction boundary and now coordinates these phases:

1. Capture and commit a source snapshot under a short claim lock. The snapshot pins the claim's optimistic version and the active documents' IDs, hashes, scan state, MIME type and storage identity. A canonical SHA-256 is recorded on the run; only that hash is exposed through the run API.
2. For each cleared document needing OCR, commit a short extraction reservation and a PENDING extraction record. Retrieve bytes, invoke OCR, structure text and store raw OCR outside a database transaction. Commit the result/fields only after rechecking the document's clearance, content hash and reservation token. Terminal extraction snapshots are not overwritten by a new attempt.
3. In a short preparation transaction, check the source snapshot, consolidate versioned extraction references, record validation/tool outcomes and construct the source-linked draft. Preparatory steps and tool/extraction evidence remain durable even if the later model/publication phase fails. They do not advance the claim's current completed-run pointer.
4. Invoke the model outside the database transaction using DTOs/source quotations, without passing live JPA entities.
5. Publish under a short claim lock. Require the same active, unexpired run/version and source fingerprint, and reverify every returned statement/citation against its stored source. Summary, statements, citations, escalation metadata and the claim's current-run pointer commit atomically. An upload, deletion or material claim/review change during model wait produces INPUTS_CHANGED and no new summary.

Document deletion now takes the same short claim lock as upload/publication so it cannot slip between source validation and summary commit. Existing officer authority checks remain in place.

## Extraction recovery

Each document reservation expires after five minutes, a development default rather than an agreed SLA. Duplicate extraction while a valid reservation exists is rejected before another paid call. A 60-second recovery sweep marks an expired PENDING extraction FAILED / EXTRACTION_INTERRUPTED and marks the document FAILED in one PostgreSQL statement. Reservation tokens and optimistic versions prevent late OCR from publishing over a replaced/recovered attempt. The job invokes no provider and does not retry automatically.

A crash can still leave a raw OCR file without a committed reference. Those files need a retention/orphan-cleanup policy; database rollback cannot undo remote file writes. Provider calls may already have been billed before an interrupted attempt, so no exactly-once billing guarantee is made.

## Migration and operation

V16 adds the run input snapshot/hash and document extraction token/expiry fields plus an expiry index. Legacy EXTRACTING rows receive an immediately expiring recovery reservation and a PENDING legacy-recovery record; the normal sweep retires these honestly. Other historical extraction and summary records remain unchanged. Stop old processors before migration; a mixed-version rolling rollout has not been accepted.

Restart the existing credential-bearing PowerShell backend once with:

```powershell
.\scripts\Start-AzureOcrPreview.ps1 -UseExistingSyntheticDatabase
```

Flyway applies V16. Reopen CLM-2026-000004 and verify summary v4, its acknowledgement, and two resolved/one unresolved findings are retained. No new paid analysis is necessary for that preservation check. Validate new provider-path behaviour on a separate synthetic case, not by overwriting the accepted demonstration history.

INPUTS_CHANGED means the case/documents or reservation changed during processing: reopen the current case, inspect its current inputs and explicitly retry. A failed publication can leave useful committed extraction/tool/preparation records attached to its failed run; these are not a published summary. Replay of an existing Idempotency-Key still returns that original run; use a new key for an intentional retry.

The API remains synchronous. This is transaction separation, not a durable queue, background worker or a claim of complete crash/chaos acceptance. The current input snapshot and prepared draft are not an automatically resumable durable job. Expired analysis reservations remain governed by V15's 30-minute deadline/recovery rules.

## Verification

Integration tests use real PostgreSQL/Flyway/storage and mocked providers with paid live flags disabled. They check:

- OCR and model callbacks observe no active Spring transaction.
- Concurrent duplicate start still invokes OCR once and reuses the active run/key alias.
- Upload and officer information-request review finish while the model is blocked; subsequent stale publication fails and the human action/prior summary remain durable.
- Document deletion while the model waits prevents that document from becoming published evidence.
- A PostgreSQL trigger deliberately aborts summary insertion. No partial summary/current-run change commits; preparation remains inspectable, prior summary survives, and explicit retry succeeds.
- Expired run recovery fences late processing; expired document extraction recovery records failure without provider calls.
- An obsolete document reservation cannot save late OCR facts; a second active extraction cannot charge another provider call.
- Existing evidence provenance, exact-version acknowledgement, current-run findings/dashboard, OpenAPI and access tests remain in the suite.

These are controlled concurrent/fault fixtures, not an actual production process-kill or multi-instance chaos exercise. Real Entra/private storage/scanning, deployment/recovery/restore acceptance and cloud budget/network decisions remain separate staging gates.

## Synthetic regression set

Ten labelled synthetic OCR-text fixtures exercise eleven field slots each: 110 presence/value checks, plus page, source-document and snippet assertions and injection flags. They include complete labels, missing/empty fields, unfamiliar policy formats, unstructured narrative, instruction-like text, whitespace/case changes, page boundaries and an out-of-policy source date kept unchanged.

The generated report is target/synthetic-structuring-acceptance.json, with a copy in docs/synthetic-structuring-acceptance-2026-11-05.json. This is a deterministic structuring regression set, authored alongside the parser. It is not independent holdout evidence, scanned-PDF OCR accuracy, model summary quality, calibrated confidence, or proof that proposed BRD accuracy targets have been met. A representative, separately labelled live OCR/model evaluation still requires agreed matching/denominator rules and reviewer acceptance.

No frontend edits, Azure resources, deployment, automatic paid provider calls, GitHub issue changes or commits are part of this sprint. Changes remain local for team review.


Final automated suite: **115 tests run, 113 passed, 0 failures/errors, 2 opt-in live-provider tests skipped**. The test log is `C:\Users\fumeh\.cache\mmaug-preview\transaction-separation-final-tests.log`. The final edge-case test also verifies that explicit OCR retry after expiry retires the prior PENDING result before the scheduled sweep.

Post-restart verification on 2026-11-05 confirmed Flyway V16 succeeded and all four new columns are present. Read-only API and PostgreSQL checks retained summary v4 ID 801b1420-83d9-4073-a0c8-1ca83ddbf56b, its exact-version acknowledgement, three current findings and two resolved findings. The dashboard reports one open finding and there are no active QUEUED/RUNNING runs. The user also reported testing v4. This accepts migration/history preservation; it does not exercise the new live provider path. No paid provider calls were made for this verification. Use a separate synthetic case for fresh OCR/model acceptance. The frontend working tree has no changes. `git diff --check` passed with existing line-ending conversion notices only.

The executable Spring Boot JAR also packaged successfully with `mvnw -DskipTests package` after the full isolated suite. Its SHA-256 is recorded in provider-transaction-test-results-2026-11-05.json. Packaging is not a deployment or authenticated staging smoke test.
