> Current increment — 5 November 2026: application-side Azure Blob/Defender-queue adapters and V17 are now implemented and tested locally. See [private-evidence-increment-2026-11-05.md](private-evidence-increment-2026-11-05.md) and its test report. Cloud resources, managed identity, real Defender delivery and private-network/cost acceptance remain pending. Older status statements below describe earlier artifacts.
> Current status — 5 November 2026: the dated observations and implementation gaps below are historical. Use [release-readiness-2026-11-05.md](release-readiness-2026-11-05.md) and [release-verification-2026-11-05.json](release-verification-2026-11-05.json) for current release evidence. Azure CLI now confirms two AI resources in the target group; real OCR/model, grounded tools/summaries and V16 transaction separation are implemented and tested. Private storage/scanning, verified Entra, private topology/cost and cloud deployment acceptance remain open. No VPN Gateway will be deployed.
# Private Azure staging readiness — 30 October 2026

Update 2 November 2026: the user prohibits VPN Gateway deployment. The earlier gateway proposal is superseded. Review the new Entra evidence and unresolved scope/client-role mapping in [entra-network-review-2026-11-02.md](entra-network-review-2026-11-02.md).
Status: NOT ready for deployment acceptance. Target is private staging with synthetic claims, as requested by the project manager. Real customer use is outside this increment. Frontend source remains unchanged; preserve the Java/Spring Boot backend and existing shared interfaces.

## Target and access

Resource group supplied by the user: `/subscriptions/9a2ab22e-5269-4da3-a16a-eeed2b0c09aa/resourceGroups/rg_mmaug_insurance`.

Azure portal inspection completed after user sign-in on 30 October 2026. The group is in North Europe; after clearing filters it shows zero resources and two successful deployment-history entries. No currently listed app, database, storage, OCR or model resource exists in this group. This does not establish whether resources exist elsewhere in the subscription.

Tenant ID: `71beefd7-29f4-4b9e-9cb2-e78b5dc851ad`. The account owns no app registrations and All applications reports insufficient privileges. Frontend/API client IDs, scopes and app roles remain unverified. The subscription ID supplied again by the user is not an application client ID.

The user set a USD 100/month ceiling and confirmed there is no existing private connection. Azure CLI is not installed locally; the earlier browser access blocker was resolved through in-app sign-in. No cloud resources, role assignments or paid deployments have been created or changed. See `infra/staging/observed-inventory.json` and `docs/private-staging-proposal.md`.

`scripts/Get-AzureStagingInventory.ps1` is a read-only inventory helper for an authenticated Azure CLI. It uses explicit subscription arguments and never lists credentials or changes the current account. Run it only after sign-in. Review existing resources before choosing names, regions, SKUs or additional services.

## Live-test review

The supplied 244.9-second recording was visually sampled at 15-second intervals. The samples show navigation, case-entry validation, case creation, empty summary/review views, and an image upload. They do not prove a completed evidence-to-summary run. Audio was not transcribed; this is not a complete narrated test report.

Code inspection established that the local OCR provider reads UTF-8 bytes, the local scanner always returns CLEAN, summary generation is deterministic, and the original orchestrator reported successful tool invocation/cross-checks/citation verification without performing them. The local preview is not production AI, and its development authentication bypass is not an access-control acceptance test.

## Implemented correction

- Consolidation remains connected to real extraction results. Validation now calls FindingService and records its returned finding count.
- A case without a cleared, extracted document fails instead of creating a seemingly successful draft.
- Tool execution, policy cross-checks and evidence verification are explicitly SKIPPED with reasons until implemented. Runs carry warnings, and template summaries state their unverified nature and missing lookups. They cannot produce an unqualified COMPLETED outcome while those limitations remain.
- A failing recorded step is persisted as FAILED before the run is marked FAILED. Response error text is generic; detailed logs remain correlated. Database transaction failure durability and asynchronous retry are still unresolved.
- Reused extraction results retain their injection flag.
- Actuator health support is installed. Liveness/readiness endpoints are tested; metrics remain protected by the security chain.
- The staging profile enables authentication, excludes automatic development seeds, and limits management exposure. A shared-environment guard rejects mixed local/dev/test profiles, disabled authentication, missing HTTPS issuer and automatic development seed imports.
- The Dockerfile uses Java 17, installs the curl used by its health check and forwards shutdown signals to Java. GitHub CI runs verification and builds an unpublished image; it contains no Azure credentials or deployment steps. Actions are pinned to revisions resolved from their upstream v4 refs.

These guards do not establish complete JWT audience validation, case isolation or a deployable production service. Shared profiles currently lack real storage, scanner and OCR implementations; startup must fail rather than reuse development stubs. Connecting external providers is still required.

## Ordered acceptance gates

| Gate | Required evidence | Existing backlog |
| --- | --- | --- |
| Inventory and design | Resource metadata, tenant/app IDs, region, private ingress plan, expected cost and authorised budget | #21–#25 |
| Identity and isolation | Officer sign-in, audience/role validation, agreed assignment policy, cross-case negatives | #27, #63, #64 |
| Private evidence and scanning | Private storage provider, quarantine/scan result handling, denied access and unavailable scanner tests | #29, #23 |
| Real document processing | Production OCR adapter, PDF/image fixtures, page/source provenance, version-aware reprocessing | #30, #67, #68 |
| Actual controlled tools | Scoped read-only results and denied/no-match/unavailable/timeout tests | #32, #51, #71 |
| Grounded summary | Provider-backed summary, runtime schema checks, valid supporting citations, missing/conflicting facts retained | #33, #52, #69 |
| Review and audit | Exact-version acknowledgement, immutable machine output, audit coverage and retention decision | #34, #35, #55–#57 |
| API acceptance | Agent endpoints match the unchanged frontend contracts and complete the recorded officer journey | #74, #75 |
| Release and recovery | Passing CI, candidate image startup, private staging smoke tests, rollback and restore evidence | #26, #36, #77, #78 |

Do not use test counts as evidence that these gates are complete. Open issues with existing code require acceptance evidence before closure. Do not deploy the current local profile or copy frontend development bypass settings into staging.

## Next execution step

Obtain the actual Entra application configuration from an app owner or tenant administrator and validate the private access route against the USD 100 budget. In parallel, finish API contract alignment and real provider adapters with HTTP/provider failure tests. Build the private staging configuration around verified existing resources, then review the exact deployment changes and cost before provisioning. No cloud provisioning is authorised by this document.

## Verification evidence

Full local Maven suite: 37 tests passed, zero failures/errors/skips. Candidate Docker image built successfully and started with an isolated PostgreSQL database; Docker health was healthy and readiness returned UP. This container smoke test used the local profile inside an isolated Docker network with no published ports, and does not establish authenticated staging acceptance. Temporary smoke containers/network were removed. The rebuilt local preview returned UP for liveness/readiness and the unchanged frontend proxy returned four existing synthetic claims. The GitHub workflow is prepared locally and has not run remotely. Azure inventory is complete; application-registration access and the costed private-access design remain open before deployment. Changes are local and uncommitted.
