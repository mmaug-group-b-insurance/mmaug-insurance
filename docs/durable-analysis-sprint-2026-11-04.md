# Durable analysis reservation and recovery sprint — 4 November 2026

Backlog: #35 (US-09 truthful processing/recovery), #82 (transaction boundaries), #74 (workflow integration), #75 (contracts). Implements a synchronous recovery foundation, not a queue/worker migration or completion of all four issues. Java/Spring Boot is preserved; frontend source remains unchanged.

## Resulting behaviour

1. An authorised analysis request first commits a QUEUED reservation in its own short transaction. It records the actor, claim, correlation, prompt/tool versions, time and a 30-minute expiry. A PostgreSQL advisory transaction lock serializes reservations for that claim; a partial unique index independently permits at most one QUEUED/RUNNING run per claim.
2. A short independent transaction marks the run RUNNING. Processing then executes under the existing claim lock and processing transaction. Concurrent starts return the existing active run ID without invoking providers again. The initiating HTTP request still waits for execution; this is not an asynchronous job queue.
3. If processing cannot commit, its transaction rolls back. A separate transaction marks the already-committed reservation FAILED with PROCESSING_ROLLED_BACK and a safe message. Prior summaries, their reviews and the prior current-run pointer survive. Failures after summary persistence escape the processing transaction so partially finalized machine output is rolled back rather than published.
4. Expected OCR/no-evidence/model failures still return honest failed outcomes and may preserve useful extraction diagnostics. A denied/unavailable lookup records a FAILED lookup step; a successful no-match response remains distinct and may still support a partial, uncertain draft.
5. A scheduler checks expired reservations every 60 seconds, starting 60 seconds after application startup. Expiry is currently 30 minutes from reservation, a development default rather than an agreed SLA. Recovery marks an expired run FAILED / PROCESS_INTERRUPTED. It uses FOR UPDATE SKIP LOCKED, increments the optimistic-lock version, and does not invoke providers. Late workers cannot overwrite a recovered version. Recovery never claims that a crash was positively identified: expiry can have other causes.
6. There are no automatic provider retries. An officer may explicitly start a new analysis after failure; retryOfRunId links it to the latest failed predecessor. Recovery preserves prior evidence/summary history and never approves, rejects or pays a claim.

## Optional request idempotency

POST `/api/v1/claims/{claimId}/analyse` still returns HTTP 202 with `{runId}`. It now also accepts optional `Idempotency-Key`: 1–100 ASCII letters/digits or `. _ : -`. Invalid keys return 422 before reserving work.

A key is scoped to the authenticated actor and claim. Its first request binds it to a run, including when that request joins an already-active run. Replaying the key returns that same run even after completion or failure, without another provider call. To intentionally retry a failed keyed operation, use a new key. Keys are identifiers, not credentials, and should not contain claim content. Aliases persist in agent_run_request; retention policy must include them.

The existing frontend sends no key. It benefits from active-run coalescing, but a later click after a completed request is still an intentional new run. A lost completed response cannot be deduplicated for an unkeyed client. No exactly-once guarantee is made for external OCR/model billing: if a provider succeeded before a transaction or process failed, an explicit retry may repeat that paid work.

## Migration and safe rollout

V15 adds nullable retry_of_run_id and expires_at to agent_run, an active-run unique index, an expiry index, and the request-alias table. The migration is additive and preserves existing summaries/reviews/findings. If a database already contains multiple active runs for a claim, index creation intentionally fails; inspect those runs and stop competing processors before an operator decides which stale records to retire. Do not delete history to force the migration through.

Restart the credential-bearing local backend session once with scripts/Start-AzureOcrPreview.ps1 -UseExistingSyntheticDatabase. Flyway applies V15. No additional Azure resource or paid deployment is required. The unchanged frontend can continue to read run status through the existing endpoints; retryOfRunId and expiresAt are additive fields.

Do not automatically regenerate the user's accepted v4 just to demonstrate the migration. Reopen that claim and verify its acknowledgement and two resolved/one unresolved findings remain. A fresh analysis creates a new version and new run-specific findings; resolutions are not silently copied to new evidence.

## Failure runbook

- RUNNING/QUEUED: inspect its run ID/status and correlation. Repeated start returns the active reservation. Do not manually resubmit providers while it is active.
- PROCESSING_ROLLED_BACK: inspect safe correlated diagnostics and database/provider availability. Prior published output remains the last committed version. Correct the cause, then explicitly retry. A keyed retry requires a new key.
- PROCESS_INTERRUPTED: inspect why the reservation exceeded expiry and whether an old process is still active. Recovery records a failed attempt rather than guessing a successful result. Explicit retry is available once the active reservation is cleared.
- Database unavailable: failure recording and recovery also require the database. The API may return an error; the committed reservation remains available for recovery after connectivity returns. Do not report a durable FAILED update that could not be committed.
- Recovery skips locked rows; it does not kill database sessions, interrupt provider calls or take over live transactions. An expired run holding database locks can remain active until those locks are released. In-memory logs or a successful process exit alone are not a recovery test.

## Transaction boundary audit / remaining work

Reservation, RUNNING transition, rollback-failure recording and expiry recovery are separate short transactions. AgentExecutionService refuses an ambient transaction. The orchestrator continues to hold the claim lock while calling OCR/model services, to preserve existing upload/review/version serialization. Per-step progress and intermediate tool/extraction records are not independently durable if processing rolls back. Raw-file writes cannot be undone by a database rollback and may require orphan cleanup.

A separate increment must move provider work outside long database transactions using durable jobs, explicit immutable input snapshots, transaction-safe publication/fencing, worker leases/heartbeats and bounded retries. Physical process-kill/restart, multi-instance deployment and connection-loss chaos exercises remain staging acceptance work. Private storage/scanning, real identity/case isolation, operational ownership and private staging cost/network decisions are still release gates.

## Test evidence

The isolated PostgreSQL/HTTP suite covers:

- Duplicate start during blocked OCR returns the same committed RUNNING run and invokes OCR once.
- A request key attached to that active run replays the same ID after completion.
- Completed keyed requests replay without another run/provider call; invalid keys fail, and the same key on another claim cannot retrieve the first claim's run.
- A real PostgreSQL division-by-zero abort rolls back processing; its reservation becomes durably FAILED, the prior summary/current-run pointer survive, and explicit retry records its predecessor.
- Expired reservation recovery is repeatable and invokes no providers; a late worker cannot overwrite PROCESS_INTERRUPTED or publish a summary.
- An error after summary persistence escapes to force rollback; unavailable tools have FAILED step status.
- V15 migrates an isolated empty database; all existing grounding, review, evidence, current-run findings, API contract and security tests remain in the suite.

Paid live OCR/model tests are disabled during this suite. These are controlled fault-injection tests, not a claim that production restart/chaos acceptance has been completed.


Final isolated suite: **107 tests run, 105 passed, 0 failures/errors, 2 opt-in live-provider tests skipped**. Log: `C:\Users\fumeh\.cache\mmaug-preview\durable-analysis-final-tests.log`. The OpenAPI snapshot includes the intentionally added optional Idempotency-Key header. `git diff --check` passed (existing line-ending conversion notices only).

Read-only verification of the still-running preview after implementation confirmed summary v4 ID 801b1420-83d9-4073-a0c8-1ca83ddbf56b, one acknowledgement, three current findings and two resolved findings. V15 and the new runtime code have not yet been applied to that preview process; local post-restart acceptance remains pending. No cloud resource, frontend file, GitHub issue state, commit or deployment was changed in this sprint.

## Local post-restart acceptance

The restarted preview successfully applied Flyway V15. Read-only inspection confirmed the active-run unique index, expiry index and request-alias index exist, and no active runs were left behind. Run API responses expose expiresAt and retryOfRunId.

Summary v4 remained 801b1420-83d9-4073-a0c8-1ca83ddbf56b; its acknowledgement still references that exact summary. Three current findings remain, two resolved, and dashboard openFindingsCount is 1. An invalid Idempotency-Key was rejected with HTTP 422 and no added run. No OCR/model analysis was triggered during this post-restart verification. Destructive fault tests remain confined to the isolated integration databases.


Later increment: [provider transaction separation � 5 November](provider-transaction-separation-2026-11-05.md) removes the OCR/model transaction span described here and adds input fingerprint/publication checks and extraction leases. This report remains the historical V15 reservation/recovery baseline.
