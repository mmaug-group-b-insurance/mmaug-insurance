> Current increment — 5 November 2026: application-side Azure Blob/Defender-queue adapters and V17 are now implemented and tested locally. See [private-evidence-increment-2026-11-05.md](private-evidence-increment-2026-11-05.md) and its test report. Cloud resources, managed identity, real Defender delivery and private-network/cost acceptance remain pending. Older status statements below describe earlier artifacts.
> Latest local increment — 6 November 2026: short encrypted PII fields now decode correctly; shared environments require a valid explicit key. See [PII increment](pii-release-increment-2026-11-06.md) and `pii-test-results-2026-11-06.json` for the current artifact and checks. Earlier checksums below describe previous artifacts. Azure Defender and staging acceptance remain open.

# Release preparation increment — 5 November 2026

Local image acceptance update — 6 November 2026: the Docker image built and passed the isolated non-root/read-only runtime smoke. See [container checkpoint](ci-container-checkpoint-2026-11-06.md) and `container-test-results-2026-11-06.json`. The image has its own embedded-JAR checksum because it was rebuilt inside Docker. Remote CI, Entra and Azure staging acceptance remain open.

## Outcome

The accepted synthetic increment now has a tested executable JAR, configuration hardening, a repeatable isolated JAR smoke test, a PostgreSQL restore rehearsal, updated CI and a release-evidence bundle. These changes remain local and uncommitted. They do not establish private Azure staging acceptance or real-customer readiness.

Business acceptance is recorded in `business-review-acceptance-2026-11-05.md`. The Java/Spring Boot backend and frontend interfaces are preserved; frontend source was not changed.

## Implemented and verified

- CORS now binds `app.security.cors.allowed-origins` instead of ignoring configuration in favour of hard-coded localhost origins. Unconfigured origins are denied. Correlation IDs remain exposed to allowed clients.
- The shared-environment guard additionally requires an explicit nonblank API audience, exact HTTPS CORS origins, Azure OCR/summary providers, Hibernate schema validation and disabled public API documentation. It rejects malformed issuer URLs and mixed development profiles. The guard does not validate a real token, grant roles or prove networking/private storage.
- Staging configuration forces Azure providers, requires audience and frontend-origin configuration, and disables Swagger/OpenAPI. `infra/staging/staging.env.example` is a configuration checklist with no secret values; it is not a complete deployment manifest.
- CI includes evaluation scoring/dry-run checks, explicitly disables paid live tests, reacts to script/evaluation changes and prints the JAR checksum. Remote GitHub CI has not been run in this sprint.
- `Test-SyntheticDatabaseRestore.ps1` dumped the synthetic preview into a private local cache directory, restored a new random database, compared full-row digests/counts of seven key tables and removed the temporary database. The source was unchanged. This is a database-only rehearsal; document blobs, Azure backup/restore, RTO/RPO and disaster recovery remain untested.
- `Test-ReleaseJar.ps1` ran the executable candidate against a newly generated isolated database with authentication enabled. Readiness returned 200, anonymous claims and metrics returned 401, allowed-origin preflight returned 200 and disallowed-origin preflight returned 403. Its child process and temporary database were removed. Local providers were used without provider calls. This does not test valid Entra tokens or shared-profile startup.
- Backend suite: **122 tests run, 120 passed, 0 failures/errors, 2 opt-in live tests skipped**. Five Python evaluator tests passed. The JAR packaged successfully after the suite.

Machine-readable evidence is in `release-verification-2026-11-05.json`. Current JAR SHA-256 is recorded there and in the candidate manifest; older sprint hashes refer to older artifacts.

## Refreshed Azure evidence

Authenticated CLI inspection on 5 November confirmed the target resource group `rg_mmaug_insurance` in subscription `9a2ab22e-5269-4da3-a16a-eeed2b0c09aa` contains two resources:

| Resource | Region | Configuration observed |
| --- | --- | --- |
| mmaug-claims-docintel-01 | East US | FormRecognizer, F0, public network enabled, default network action Allow, no private endpoints |
| mmaug-claims-openai-01 | East US | OpenAI, S0, public network enabled, default network action Allow, no private endpoints |

Model deployment `gpt-6.1-sol`: Succeeded, model/version `gpt-6.1-sol` / `2026-09-29`, GlobalStandard, capacity 250. These are observed resource properties, not a fresh price estimate or a guarantee of future model availability.

No application hosting, PostgreSQL, storage account or private endpoint resource is listed in this resource group. This inspection does not prove absence elsewhere in the subscription. Reading the Entra application via Graph still returns insufficient privileges. The supplied app/tenant IDs remain known configuration, but current role definitions, assignments and redirect URLs are not verified by this CLI account.

The inventory supersedes the 30 October empty-resource-group observation in the earlier proposal. No cloud resources, network settings, identities or deployment configuration were changed. No keys/tokens were listed. USD 100/month and no VPN Gateway remain explicit constraints; no new costed private-access design has been accepted.

## Dashboard and frontend-owner handoff

The dashboard queries claims with workflow status ESCALATED. Agent-run ESCALATED means an AI recommendation; it deliberately does not set the officer's workflow status. Officer review ESCALATED_TO_SENIOR sets that claim status. Therefore zero workflow escalations alongside an escalated run is consistent with current code. Acknowledgement sets READY_FOR_REVIEW and does not constitute escalation to a senior.

Coordinate with the frontend owner to distinguish “AI escalation recommended” from “Escalated by officer” in labels, and improve faint amber escalation text. Do not silently map a model recommendation into an officer decision. Consider a future separate recommendation count/API only after agreeing the contract.

## Remaining staging gates, in order

1. **Identity/access** — app owner confirms API audience, SPA config, scope, role values and assignments; exercise real officer sign-in and negative role/case-access tests. Current officers can access all cases; per-assignment isolation is not implemented and requires an agreed policy.
2. **Evidence storage/scanning** — implement and accept a private Azure Blob provider and a real malware-scanning/quarantine flow. Only local file storage and always-clean development scanning currently exist. The staging profile has no fallback for these beans and must not be made runnable by enabling a local profile.
3. **Private topology/budget** — agree how team browsers reach private staging without an existing private connection or VPN Gateway, verify SKUs/region/usage/alerts and review a concrete cost estimate within USD 100. Current AI services are publicly network-accessible; authentication alone does not make them network-private.
4. **Release/cloud acceptance** — run remote CI and container checks, use immutable image digests, preview infrastructure changes, then verify staged health/auth/upload/scan/OCR/summary/review, blob+database restore and rollback. Current synchronous processing is not an automatically resumable durable queue.
5. **Broader quality acceptance** — retain the accepted four-case regression evidence, and evaluate a separately labelled, diverse synthetic holdout set with agreed scoring/thresholds. Business rules and confidence remain synthetic/uncalibrated.

Docker CLI exists but its Linux engine is not running; no current candidate container build/smoke result is claimed. Earlier container evidence belongs to an earlier artifact.

## Candidate and restart

`New-ReleaseCandidate.ps1 -OutputDirectory <new-directory-outside-repo>` assembles only an explicit allowlist of JAR, evidence and public configuration template files, with hashes. It excludes `.env`, credentials, database dumps and document storage. Because source changes are uncommitted, the manifest records the base commit and dirty state; this is a local candidate, not a reproducible reviewed Git release.

The running preview was not restarted or interrupted. To use this candidate locally, restart the existing credential-bearing backend PowerShell using the usual `Start-AzureOcrPreview.ps1 -UseExistingSyntheticDatabase`. No new database migration is introduced by this increment. The release smoke test used the new JAR independently.

Rollback strategy for this increment: retain the previous tested JAR/config and redeploy it if the new configuration fails. There is no schema rollback because this increment adds no migration. Actual cloud revision rollback remains pending an application deployment platform. Never treat a PostgreSQL restore alone as evidence that blob data or cross-resource consistency has been recovered.
