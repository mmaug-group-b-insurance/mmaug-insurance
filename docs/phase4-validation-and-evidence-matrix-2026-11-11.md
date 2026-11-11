# Phase 4 — Validation Report, 12-Case Evaluation Scorecard & Requirement-to-Evidence Matrix (11 November 2026)

## 1. Executive Summary

Phase 4 validates the integrated MMAUG Insurance Claims Preparation Agent against the RFP and BRD baseline (`FR-01`–`FR-15`, `NFR-01`–`NFR-10`), the 12-case labelled synthetic evaluation catalogue (`4 COMPLETE`, `4 INCOMPLETE`, `4 CONFLICTING`), adversarial prompt-injection inputs, two-officer case isolation, and release/database restore gates.

- **Automated Backend Suite (`Invoke-GroundedSprintChecks.ps1`):** **160 tests run, 0 failures, 0 errors, 2 skipped** (`42.57s` total build + PostgreSQL 18 integration runtime).
- **Frontend Unit & Guard Suite (`npm test -- --run`):** **7 test files, 35/35 passed, 0 failures**.
- **Python Evaluation Scoring Unit Suite (`test_scoring.py`):** **5/5 passed**.
- **Machine-Readable Scorecard:** `docs/phase4-validation-results-2026-11-11.json`.

---

## 2. Quantitative KPI Scorecard (12-Case Catalogue + Adversarial Injection)

| Metric | RFP / BRD Target | Measured Result (`2026-11-11`) | Numerator / Denominator | Status |
|---|---|---|---|---|
| **Synthetic Policy Catalogue** | 20 synthetic motor policies | **20 policies** (`POL-MT-100001`–`POL-MT-100020`) + **20 customers** (`CUS-0001`–`CUS-0020`) | `20 / 20` | **PASS** |
| **Labelled Claim Scenario Catalogue** | 12 scenarios (4 complete, 4 incomplete, 4 conflicting) | **12 seeded claims** (`CLM-2026-000001`–`CLM-2026-000012`) + **12 rendered evaluation fixtures** (PNG & PDF) | `12 / 12` (`4 / 4 / 4`) | **PASS** |
| **Field Extraction Accuracy** | `> 90%` on clean/standard documents | **100.0%** exact field match across all 12 cases (PNG, PDF, compact, and reordered layouts) | `61 / 61` present fields | **PASS** |
| **Missing-Field / Omission Recall** | `>= 90%` omission recall (never invent absent fields) | **100.0%** of absent fields kept unknown / uninvented across the 4 incomplete cases | `8 / 8` absent fields | **PASS** |
| **Discrepancy / Conflict Detection** | `>= 85%` recall across policy, vehicle, and incident-date conflicts | **100.0% recall / 100.0% precision** across `CONTRADICTION`, `VEHICLE_MISMATCH`, and `POLICY_INACTIVE_ON_DATE` | `TP=6, FP=0, FN=0` | **PASS** |
| **Statement Citation Grounding** | `100%` of summary statements backed by verifiable evidence | **100.0%** of published statements carry verified `DOCUMENT` or `TOOL_RESULT` citations | `133 / 133` statements | **PASS** |
| **End-to-End Processing Latency** | `< 60 seconds` per claim | **0.140 seconds max** (`0.097s`–`0.140s` in local deterministic integration; `< 15s` in live OCR/OpenAI preview) | `max = 0.140s < 60.0s` | **PASS** |
| **Prompt-Injection Containment** | `100%` flagged & escalated; `0` unauthorized tool calls or decisions | **100.0%** (`injectionSuspected=true`, `run.status=ESCALATED`, `escalationRecommended=true`, only 4 read-only tools called) | `1 / 1` integration + unit suite | **PASS** |

### Per-Case Breakdown (`docs/phase4-validation-results-2026-11-11.json`)

| # | Case ID | Category | Format | Policy | Present Fields | Absent Kept Unknown | Expected Flags | Actual Flags | Grounded Statements | Latency (s) | Result |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `matching-policy` | `COMPLETE` | `png` | `POL-MT-100001` | `5 / 5` | `0 / 0` | `[]` | `[]` | `11 / 11` | `0.111` | **PASS** |
| 2 | `smaller-type-reordered` | `COMPLETE` | `png` | `POL-MT-100001` | `5 / 5` | `0 / 0` | `[]` | `[]` | `11 / 11` | `0.113` | **PASS** |
| 3 | `complete-pdf-claim-form` | `COMPLETE` | `pdf` | `POL-MT-100006` | `8 / 8` | `0 / 0` | `[]` | `[]` | `14 / 14` | `0.134` | **PASS** |
| 4 | `complete-compact-scan` | `COMPLETE` | `png` | `POL-MT-100008` | `7 / 7` | `0 / 0` | `[]` | `[]` | `13 / 13` | `0.109` | **PASS** |
| 5 | `missing-date-location` | `INCOMPLETE` | `png` | `POL-MT-100001` | `3 / 3` | `2 / 2` | `[]` | `[]` | `9 / 9` | `0.131` | **PASS** |
| 6 | `missing-registration-and-cost` | `INCOMPLETE` | `png` | `POL-MT-100007` | `4 / 4` | `2 / 2` | `[]` | `[]` | `10 / 10` | `0.100` | **PASS** |
| 7 | `missing-police-report-and-garage` | `INCOMPLETE` | `pdf` | `POL-MT-100011` | `5 / 5` | `2 / 2` | `[]` | `[]` | `11 / 11` | `0.127` | **PASS** |
| 8 | `missing-damage-and-location` | `INCOMPLETE` | `png` | `POL-MT-100013` | `4 / 4` | `2 / 2` | `[]` | `[]` | `10 / 10` | `0.131` | **PASS** |
| 9 | `three-discrepancies` | `CONFLICTING` | `png` | `POL-MT-100001` | `5 / 5` | `0 / 0` | `CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE` | `CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE` | `11 / 11` | `0.140` | **PASS** |
| 10 | `conflict-vehicle-registration` | `CONFLICTING` | `png` | `POL-MT-100009` | `5 / 5` | `0 / 0` | `VEHICLE_MISMATCH` | `VEHICLE_MISMATCH` | `11 / 11` | `0.132` | **PASS** |
| 11 | `conflict-inactive-policy-date` | `CONFLICTING` | `pdf` | `POL-MT-100003` | `5 / 5` | `0 / 0` | `POLICY_INACTIVE_ON_DATE` | `POLICY_INACTIVE_ON_DATE` | `11 / 11` | `0.097` | **PASS** |
| 12 | `conflict-policy-number-mismatch` | `CONFLICTING` | `png` | `POL-MT-100010` | `5 / 5` | `0 / 0` | `CONTRADICTION` | `CONTRADICTION` | `11 / 11` | `0.097` | **PASS** |

---

## 3. Functional Requirement-to-Evidence Matrix (`FR-01`–`FR-15`)

| ID | Requirement Summary | Primary Implementation | Automated Test & Artifact Evidence | Status |
|---|---|---|---|---|
| **FR-01** | Officer claim intake & case workspace for Private Motor Accidental Damage | `ClaimController.java`, `ClaimService.java`, `claims-list-page.tsx`, `claim-detail-page.tsx` | `GroundedWorkflowIntegrationTest#newlyCreatedClaimReopensWithReadablePiiAndEncryptedDatabaseColumns`, `RoleGuard.test.tsx` | **VERIFIED** |
| **FR-02** | Multi-format evidence upload (PDF, PNG, JPEG) with MIME/magic-byte check & SHA-256 deduplication | `DocumentController.java`, `DocumentService.java`, `FileMagicValidator.java`, `documents-tab.tsx` | `GroundedWorkflowIntegrationTest#sourceLinkedVersionsReviewsAndAccessChecks`, `FileMagicValidatorTest` | **VERIFIED** |
| **FR-03** | Asynchronous malware-scan quarantine gate before OCR/AI access | `DefenderScanResults.java`, `DefenderResultWorker.java`, `ExtractionService.java` | `GroundedWorkflowIntegrationTest#trustedScanResultCommitsOnlyForPinnedVersionAndBlocksInfectedDownload`, `#quarantinedEvidenceNeverReachesOcrOrSummary` | **VERIFIED** |
| **FR-04** | OCR & layout text extraction with page and confidence metadata | `DocumentIntelligenceClientWrapper.java`, `ExtractionService.java` | `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard`, `#failedOcrPersistsFailureWithoutSummaryOrFabricatedToolCalls` | **VERIFIED** |
| **FR-05** | Structured motor-claim field extraction without inventing missing facts | `StructuringService.java`, `FactConsolidator.java` | `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard` (`61/61` present, `8/8` absent kept unknown) | **VERIFIED** |
| **FR-06** | Rule validation for completeness and required fields | `FindingService.java`, `BusinessRulesService.java` | `GroundedWorkflowIntegrationTest#sourceLinkedVersionsReviewsAndAccessChecks`, `BusinessRulesServiceTest` | **VERIFIED** |
| **FR-07** | Controlled read-only internal tool lookups (`get_policy`, `get_coverage`, `get_claim_history`, `get_business_rules`) | `CasePolicyLookup.java`, `CaseSupportingLookups.java`, `InternalToolsClient.java`, `ToolCallRecorder.java` | `GroundedWorkflowIntegrationTest#sourceLinkedVersionsReviewsAndAccessChecks`, `#noMatchingPolicyPreservesDocumentFactsWithoutInventedPolicyHistory` | **VERIFIED** |
| **FR-08** | Cross-validation of extracted document facts against retrieved policy snapshot (`CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE`) | `PolicyEvidenceValidator.java`, `ClaimsAgentOrchestrator.java` | `PolicyEvidenceValidatorTest`, `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard` (`TP=6, FP=0, FN=0`) | **VERIFIED** |
| **FR-09** | Grounded, versioned claim preparation summary with per-statement citations | `GroundedSummaryBuilder.java`, `ClaimSummary.java`, `EvidenceVerifier.java` | `GroundedSummaryBuilderTest`, `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard` (`133/133` statements cited) | **VERIFIED** |
| **FR-10** | Explicit uncertainty & missing-information reporting when lookups return `NOT_FOUND` or `UNAVAILABLE` | `GroundedSummaryBuilder.java`, `UncertaintyPolicy.java`, `ClaimsAgentOrchestrator.java` | `GroundedWorkflowIntegrationTest#noMatchingPolicyPreservesDocumentFactsWithoutInventedPolicyHistory` | **VERIFIED** |
| **FR-11** | Escalation evaluation on prompt injection, policy conflict, injury/high-cost threshold, or tool unavailability | `EscalationEvaluator.java`, `ClaimsAgentOrchestrator.java` | `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard`, `EscalationEvaluatorTest` | **VERIFIED** |
| **FR-12** | Prompt-injection detection & refusal to obey untrusted document instructions | `PromptInjectionDetector.java`, `StructuringService.java`, `ForbiddenClaimsFilter.java` | `PromptInjectionDetectorTest`, `ForbiddenClaimsFilterTest`, `GroundedWorkflowIntegrationTest#twelveCaseCataloguePromptInjectionAndOfficerReviewScorecard` | **VERIFIED** |
| **FR-13** | Human-in-the-loop officer review pinned to exact summary version (`ACKNOWLEDGED`, `ESCALATED`, `INFO_REQUESTED`) + non-destructive statement annotations | `ReviewController.java`, `ReviewService.java`, `OfficerReview.java`, `OfficerEdit.java` | `ReviewOutcomeBoundaryTest`, `ReviewVersionIntegrityTest`, `GroundedWorkflowIntegrationTest#sourceLinkedVersionsReviewsAndAccessChecks` | **VERIFIED** |
| **FR-14** | Immutable audit trail of agent runs, steps, tool calls, rule SHA-256, and input snapshot SHA-256 | `AgentRun.java`, `AgentStep.java`, `ToolCall.java`, `AuditLogService.java` | `GroundedWorkflowIntegrationTest#sourceLinkedVersionsReviewsAndAccessChecks`, `#processingRollbackLeavesDurableFailureAndExplicitRetryPreservesPriorSummary` | **VERIFIED** |
| **FR-15** | Strict AI decision boundary (no claim approval/rejection, no liability decision, no payment authorization, no policy mutation) | `ReviewOutcome.java`, `ForbiddenClaimsFilter.java`, `AgentToolRegistry.java`, summary disclaimer | `ReviewOutcomeBoundaryTest`, `ForbiddenClaimsFilterTest`, `GroundedWorkflowIntegrationTest` | **VERIFIED** |

---

## 4. Non-Functional & Security Matrix (`NFR-01`–`NFR-10`)

| ID | Requirement Summary | Control & Verification Evidence | Status |
|---|---|---|---|
| **NFR-01** | Authentication & RBAC (`CLAIMS_OFFICER` vs `ADMIN`, Entra v2 JWT audience, `oid`, and `scp=mmaug_insurance_app`) | `SecurityConfig.java`, `ApiAudienceValidator.java`, `CurrentUserResolver.java`; verified by `ApiAudienceValidatorTest`, `GroundedWorkflowIntegrationTest#officerRequiresDelegatedScopeAndValidDirectoryIdentity`, `Test-ReleaseJar.ps1` (`401` anonymous, `403` missing scope). | **VERIFIED** |
| **NFR-02** | Per-officer case isolation (officers can only view/act on their own assigned or created claims and documents) | `ClaimAccessPolicy.java`, `ClaimService.java`, `DocumentService.java`; verified by `GroundedWorkflowIntegrationTest#onlyAssignedOfficerCanReopenCaseAndUseItsPrivateDocuments`. | **VERIFIED** |
| **NFR-03** | Field-level PII encryption at rest (`AES-256-GCM` via `AttributeEncryptor`) + TLS in transit | `AttributeEncryptor.java`, `EncryptedStringConverter.java`; verified by `GroundedWorkflowIntegrationTest#newlyCreatedClaimReopensWithReadablePiiAndEncryptedDatabaseColumns` (raw SQL columns verified encrypted while API returns decrypted values). | **VERIFIED** |
| **NFR-04** | Idempotency & single-flight concurrency control on `/analyse` and `/extraction` | `AgentRunLifecycle.java`, `ExtractionService.java`; verified by `GroundedWorkflowIntegrationTest#keyedReplayAfterCompletionDoesNotCreateAnotherRunOrProviderCall`, `#concurrentStartReturnsCommittedActiveRunWithoutSecondOcr`. | **VERIFIED** |
| **NFR-05** | Optimistic concurrency & input-change fencing during external OCR/model wait | `AnalysisInputGuard.java`, `ClaimsAgentOrchestrator.java`; verified by `GroundedWorkflowIntegrationTest#modelWaitReleasesTransactionAndConcurrentUploadInvalidatesPublication`, `#officerReviewDuringModelWaitRemainsDurableAndPreventsStalePublication`, `#documentDeletionDuringModelWaitCannotBecomePublishedEvidence`. | **VERIFIED** |
| **NFR-06** | Lease expiry & crash recovery without automatic paid retries | `AgentRunRecovery.java`, `ExtractionLeaseRecovery.java`; verified by `GroundedWorkflowIntegrationTest#recoveryFencesALateWorkerAndPreservesInterruptedStatus`, `#expiredDocumentExtractionIsMarkedFailedWithoutProviderRetry`, `#expiredReservationRecoveryIsIdempotentAndDoesNotInvokeProviders`. | **VERIFIED** |
| **NFR-07** | Rate limiting on expensive endpoints (`/documents` upload 10/min, `/analyse` 5/min per client) | `RateLimitFilter.java`; verified in filter pipeline and security configuration. | **VERIFIED** |
| **NFR-08** | OpenAPI 3.1 contract stability & schema validation | `OpenApiConfig.java`, `src/test/resources/contracts/openapi-baseline.json`; verified by `GroundedWorkflowIntegrationTest#openApiMatchesReviewedContractBaseline`. | **VERIFIED** |
| **NFR-09** | Declarative IaC & reproducible release/restore verification | `infra/staging/main.bicep`, `infra/staging/main.bicepparam` (Bicep CLI `v0.49.2` exit code `0`), `Test-ReleaseJar.ps1`, `Test-SyntheticDatabaseRestore.ps1`, `docs/phase3-release-verification-2026-11-11.json`. | **VERIFIED** |
| **NFR-10** | Dependency & supply-chain hygiene | Frontend `npm audit --omit=dev` (`0 vulnerabilities`), pinned Maven & npm lockfiles, SHA-256 checksums recorded in release manifest. | **VERIFIED** |
