# Private evidence and Defender clearance increment — 5 November 2026

## Scope

Implements the application adapters supporting FR-03, FR-04, FR-11 and NFR-01/02/04. The Java backend and frontend contract are preserved. This is not a claim that private Azure storage or real malware scanning has been deployed/accepted. No cloud resources, paid plans, keys or role assignments were created or changed.

The existing local file provider and always-clean scanner remain confined to local/dev/test profiles and their selected provider settings. Shared profiles require Azure storage and Defender-queue clearance; there is no always-clean shared fallback.

## Implemented behaviour

- Azure Blob SDK clients authenticate using ManagedIdentityCredential only. No account key, connection string, SAS, Azure CLI or interactive credential fallback is accepted by this adapter. Optional user-assigned identity client ID is configuration, not a secret.
- Endpoints must be standard Azure public-cloud HTTPS storage hostnames. These hostnames can resolve through private DNS; their format alone does not prove private network access. Sovereign-cloud/custom-domain endpoints are outside this increment.
- Only `quarantine` and `clean` logical containers are allowed. The adapter checks container access and refuses a publicly readable container; it never creates containers or changes their permissions. Private networking and account-level public-access disablement must be provisioned separately.
- Writes use If-None-Match `*`, preventing this code from overwriting an existing blob. No content tags or scan verdicts are written by the application.
- The upload records its opaque storage ETag alongside the SHA-256. Reads for download and OCR use If-Match for that exact version. Before paid OCR, externally stored bytes also have their SHA-256 checked against the upload digest, bounded to the existing 15 MB implementation limit. This does not change the proposed BRD file-limit decision.
- Downloads remain behind existing case authorisation and now reject all non-CLEAN originals with UNSAFE_FILE/422. No SAS URL is issued. The frontend source is unchanged; quarantine is visible in existing scan/processing fields.
- Azure uploads return PENDING/QUARANTINED. Defender on-upload scanning is asynchronous; upload success never means scan success. Scan-result arrival does not start OCR/model analysis automatically.
- The scheduled queue reader takes at most ten messages per poll (30-second delay, 120-second visibility), with bounded SDK retries/timeouts. It handles Event Grid schema objects or arrays of up to ten objects, using Base64 queue-message encoding. Configure and verify that encoding/schema in live acceptance.
- Clearance requires the exact configured topic, Microsoft.Security.MalwareScanningResult event type, configured storage host, original quarantine object, recorded ETag, valid scan timestamp and current matching blob ETag. Network version lookup happens outside the short publication transaction. Database publication locks the case and document and rechecks state/version/deletion.
- Only the exact `No threats found` verdict clears a PENDING version. Malicious becomes INFECTED/FAILED; errors, unsupported or unknown verdicts become REJECTED/QUARANTINED. Missing results stay pending. Negative verdicts do not require the blob to remain readable because Defender can soft-delete malicious objects.
- Duplicate clean results do not reset extraction state. Rejected/infected versions cannot be re-promoted by a clean replay; a malicious result can block a previously clean version. Scan event ID, finished time and safe result code are retained internally. No malware names/raw messages are logged.
- Messages are deleted only after handling completes. Unknown document/version-publication failures remain available for retry; wrong-source/malformed events are discarded without clearing anything. Events for internal `extractions/` artifacts are ignored because they are not original case uploads.

## Event trust boundary and operational limits

Queue authentication is mandatory. Expected topic text and event payload fields are consistency checks, **not proof of sender identity**. The results queue must allow only the intended Event Grid delivery identity to enqueue and the backend identity to receive/delete. The backend must not have results-queue send rights. Restrict who can publish to the configured custom topic to the intended Defender integration. Enabling shared-key/SAS access or granting broad queue/topic writer roles would break this trust model. No public webhook or model-generated clearance is accepted.

Blob index tags are not used for clearance: Microsoft explicitly warns they are mutable and should not be the sole control for a security-sensitive workflow. See [Defender scanning and index-tag limitations](https://learn.microsoft.com/en-us/azure/defender-for-cloud/introduction-malware-scanning), [scan result event schema](https://learn.microsoft.com/en-us/azure/defender-for-cloud/defender-for-storage-configure-malware-scan) and [queue event delivery](https://learn.microsoft.com/en-us/azure/event-grid/handler-storage-queues).

There is no automatic scan re-request, dead-letter queue mover, pending-document expiry or durable analysis queue in this increment. Deferred/orphan messages need monitored receive counts, queue depth/age and an operator procedure before shared acceptance. Event Grid delivery dead-lettering and consumer poison-message handling are different concerns; both need configuration/acceptance. Queue retention/visibility and scan latency must be tested under realistic conditions. No paid analysis is automatically retried.

Upload storage I/O still occurs within the existing synchronous upload transaction/claim lock. Defender verdict lookup is decoupled, but this is not a claim that every upload network operation is transaction-free. A failed upload/commit can leave an orphan private blob; reconcile and approve retention/cleanup before shared use. Source blobs remain physically in quarantine after clearance, with application status and pinned version controlling access; there is no untested copy-to-clean promotion.

## Migration and compatibility

V17 adds nullable `storage_etag`, `scan_event_id`, `scan_finished_at` and `scan_result_code` fields plus a lookup index. It does not rewrite historical summaries, reviews, extraction results or local scan states. Historical local documents have no external version. Analysis snapshot hashes include ETag only where present, preserving the prior local snapshot shape.

Stop old processors before migration; do not mix old and new workers. Apply through the normal backend restart/Flyway path. The current preview has not been restarted by this increment. V17 has been tested in isolated databases, not yet accepted on the preview. Rolling back the app does not require dropping these additive columns; retain historical migration records and take a verified backup first.

## Verification and remaining live gate

Automated evidence is recorded in `private-evidence-test-results-2026-11-05.json`. Azure SDK calls and queue events are mocked in adapter tests; workflow tests use real PostgreSQL/Flyway, storage and HTTP with synthetic data. No real Defender scan, Azure Blob/queue integration or managed-identity token flow is claimed by these tests.

Final suite: **145 tests run, 143 passed, 0 failures/errors, 2 opt-in live-provider tests skipped**. The executable JAR packaged successfully. An isolated copy of the earlier V16 synthetic database migrated to V17 under that JAR, with identical full-row digests for accepted summary/review/finding history. Readiness and authentication/CORS smoke checks passed. The temporary process/database were removed; the running preview was untouched.

Before accepting the cloud path, provision/review:

1. A standard supported Blob storage account with account-level public blob access disabled, shared-key access disabled, HTTPS/TLS policy, private connectivity/DNS and private `quarantine`/`clean` containers. Choose location/SKU and backup/retention with the team.
2. Defender on-upload scanning enabled at the approved scope, a result topic and Event Grid-schema subscription to the restricted queue. Verify event source restrictions, delivery identity, Base64 encoding, retries, dead-letter storage and monitoring. Do not set a subscription-wide paid plan implicitly.
3. Backend managed identity with scoped blob read/write and container-property access; results-queue message processing permission without enqueue rights. Event Grid delivery gets queue-send permission only. Administrative topic publishers and shared credentials must be restricted.
4. A cost estimate covering storage operations, Defender plan/scanned volume, Event Grid, queue polling, private connectivity/DNS and hosting within the USD 100/month budget. No VPN Gateway.
5. Synthetic live acceptance: clean document, approved antivirus test fixture in an isolated scanner test, not-scanned/error result, absent scan result, storage/queue permission denial, overwritten blob, duplicate/out-of-order messages and delayed event. Verify unsafe cases never reach download/OCR/model; verify real exact-version review after a clean result.

Real credentials stay in backend secret references. Use `infra/staging/staging.env.example` for public configuration names. Azure provisioning and the broader Entra/private hosting gates remain open.
