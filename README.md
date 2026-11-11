# MMAUG Insurance Claims Preparation Agent (Group B)

**Customer:** MMAUG Insurance Ltd, Malta  
**Domain:** Motor Accidental Damage Claims Preparation  
**Reference:** Participant RFP `MMAUG-AIDB-RFP-003` · BRD v1.1 · System Design & Architecture v1.1  
**Release Target:** Friday Submission Candidate (`11 November 2026`)

---

## 1. Executive Overview & AI Decision Boundaries

The MMAUG Insurance Claims Preparation Agent assists internal **Claims Officers** with the mechanical, labor-intensive intake and preparation of private motor accidental damage claims. It automates:
- Document ingestion, MIME/magic-byte validation, and anti-malware quarantine checks.
- OCR text extraction and structured claim fact parsing.
- Automated completeness checks and missing-field identification.
- Controlled internal tool lookups for policy coverage, claim history, and business rules.
- Cross-validation between uploaded document facts and system policy records (`CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE`).
- Grounded, evidence-backed summary drafting with deterministic citations for human officer review.

### Strict AI Boundaries (Human-in-the-Loop)
Under strict regulatory and business guidelines, the AI Agent operates strictly in an **advisory preparation capacity**:
- **NEVER** auto-approves or auto-rejects claims.
- **NEVER** determines liability or fault.
- **NEVER** authorises payments or schedules disbursements.
- **NEVER** mutates or modifies insurance policies.
- **NEVER** obeys document-embedded instructions (adversarial prompt injections are flagged, contained, and escalated).
- **Officer Review Pinning:** Officer review actions (`ACKNOWLEDGED`, `ESCALATED`, `INFO_REQUESTED`) are bound to the exact immutable version of the claim summary. Any subsequent analysis run produces an incremented version, preserving full historical auditability.

---

## 2. Solution Architecture & Key Technical Decisions

The system is delivered as a robust, production-grade cloud and on-premises compatible application:
- **Backend:** Java 17, Spring Boot 4, Spring Security 6 (OIDC / OAuth2 resource server), Spring Data JPA, Flyway migrations, Jackson JSON.
- **Frontend:** React 19, TypeScript 5.9, Vite, Tailwind CSS, Lucide icons, React Router, Vitest.
- **Database & Storage:** PostgreSQL 18 / 16 with schema migrations (`V1`–`V17`) and repeatable synthetic catalogue seeds (`R__`), field-level PII encryption (`AES-256-GCM`), and Azure Blob Storage (or local storage fallback).
- **Security & RBAC:** Entra ID v2 OIDC tokens with delegated scope `scp=mmaug_insurance_app`, valid directory `oid`, and role enforcement (`ROLE_CLAIMS_OFFICER`, `ROLE_ADMIN`).
- **Per-Officer Case Isolation:** Claims officers can only access and review claims assigned to them or created by them. Colleague access returns HTTP 403 Forbidden.
- **Infrastructure as Code (IaC):** Declarative Azure Bicep definitions in [`infra/staging/main.bicep`](infra/staging/main.bicep) and [`infra/staging/main.bicepparam`](infra/staging/main.bicepparam), fully validated with Bicep CLI v0.49.2.

---

## 3. Synthetic Catalogue (20 Policies & 12 Claims)

To satisfy RFP requirements without exposing customer PII, the project ships with a verified synthetic dataset:
- **20 Synthetic Customers:** `CUS-0001` through `CUS-0020` (in `R__seed_customers.sql`).
- **20 Synthetic Motor Policies:** `POL-MT-100001` through `POL-MT-100020` (in `R__seed_policies.sql`).
- **12 Labelled Claim Scenarios:** Seeded in `R__seed_sample_claims.sql` and evaluated in [`tests/evaluation/live-cases.json`](tests/evaluation/live-cases.json):

| Scenario ID | Category | Policy Number | Incident Date | Format | Description / Intended Evaluation |
|---|---|---|---|---|---|
| `CLM-2026-000001` (`matching-policy`) | **COMPLETE** | `POL-MT-100001` | `2026-09-15` | PNG | Standard claim form; all fields present, perfect policy match. |
| `CLM-2026-000002` (`smaller-type-reordered`) | **COMPLETE** | `POL-MT-100001` | `2026-09-15` | PNG | Small font (23pt), reordered layout; tests OCR robustness. |
| `CLM-2026-000004` (`complete-pdf-claim-form`) | **COMPLETE** | `POL-MT-100006` | `2026-10-04` | PDF | Multi-line PDF form with repair cost and policyholder name. |
| `CLM-2026-000005` (`complete-compact-scan`) | **COMPLETE** | `POL-MT-100008` | `2026-10-12` | PNG | Compact layout with garage name and driver details. |
| `CLM-2026-000006` (`missing-date-location`) | **INCOMPLETE** | `POL-MT-100001` | `2026-09-15` | PNG | Missing incident date and location; verified kept unknown. |
| `CLM-2026-000007` (`missing-registration-and-cost`) | **INCOMPLETE** | `POL-MT-100007` | `2026-10-08` | PNG | Missing vehicle registration and repair cost; no hallucination. |
| `CLM-2026-000008` (`missing-police-report-and-garage`) | **INCOMPLETE** | `POL-MT-100011` | `2026-10-14` | PDF | PDF missing police report number and garage name. |
| `CLM-2026-000009` (`missing-damage-and-location`) | **INCOMPLETE** | `POL-MT-100013` | `2026-10-18` | PNG | Missing damage description and incident location. |
| `CLM-2026-000003` (`three-discrepancies`) | **CONFLICTING** | `POL-MT-100001` | `2026-09-15` | PNG | Form claims `POL-MT-100002`, registration `ZZZ999`, date `1900-01-01` (`CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE`). |
| `CLM-2026-000010` (`conflict-vehicle-registration`) | **CONFLICTING** | `POL-MT-100009` | `2026-10-10` | PNG | Registration mismatch (`ZZ 9999` vs policy `VWX 234`). |
| `CLM-2026-000011` (`conflict-inactive-policy-date`) | **CONFLICTING** | `POL-MT-100003` | `2026-10-05` | PDF | Incident date outside active policy period. |
| `CLM-2026-000012` (`conflict-policy-number-mismatch`) | **CONFLICTING** | `POL-MT-100010` | `2026-08-01` | PNG | Claim form policy number conflicts with case policy number. |

---

## 4. Quick Start & Clean Environment Verification

### Prerequisites
- **Java Development Kit (JDK):** Version 17 (`JAVA_HOME` pointing to JDK 17).
- **Node.js & npm:** Node 20+ (`npm` 10+).
- **PostgreSQL Database:** PostgreSQL 16 or 18 running locally, or Docker daemon if using Testcontainers.
- **PowerShell:** Version 5.1+ (Windows) or PowerShell Core (macOS/Linux).

### Running Automated Test Suites (Offline / Without Incurring Paid Cloud Charges)
The entire test suite runs offline using deterministic mocked OCR/model services and isolated temporary databases:

1. **Full Backend Integration & 12-Case Scorecard:**
   ```powershell
   .\scripts\Invoke-GroundedSprintChecks.ps1 -JdkHome 'C:\path\to\jdk-17'
   ```
   *Expected outcome:* **160 tests run, 0 failures, 0 errors, 2 skipped**. Evaluates all 12 cases, prompt injection, idempotency, officer review pinning, and case isolation.

2. **Frontend Unit & Accessibility Tests:**
   ```powershell
   cd frontend
   npm ci
   npm test -- --run
   npm run typecheck
   ```
   *Expected outcome:* **7 test files passed, 35/35 tests clean, 0 type errors**.

3. **Release JAR Build & Synthetic Database Restore Verification:**
   ```powershell
   .\scripts\Test-ReleaseJar.ps1 -RestoreSyntheticSnapshot
   .\scripts\Test-SyntheticDatabaseRestore.ps1
   ```
   *Expected outcome:* Compiles production JAR, starts temporary server, validates HTTP security headers, anonymous 401s, preflight CORS, and tests restore integrity without altering source data.

---

## 5. Running the Application Locally (Demonstration Mode)

### Step 1: Start PostgreSQL Preview Instance
If not using an existing PostgreSQL instance, launch the local native instance or Docker container:
```powershell
# Using the pre-configured local preview runner:
.\scripts\Start-AzureOcrPreview.ps1
```

### Step 2: Run Spring Boot Backend
```powershell
cd mmaug-claims-agent-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=preview
```
Backend starts on `http://localhost:8081` (Swagger OpenAPI docs at `http://localhost:8081/swagger-ui.html`).

### Step 3: Run React Frontend
```powershell
cd frontend
npm run dev
```
Frontend serves at `http://localhost:5173`.

### Demo Credentials & Officer Setup
In preview/development mode, mock officer profiles are available via the user menu:
- **Officer 1 (Assigned Case Lead):**
  - OID: `00000000-0000-0000-0000-000000000001`
  - Name: Sarah Borg
  - Role: `ROLE_CLAIMS_OFFICER`
  - Access: Assigned claims `CLM-2026-000001`, `000003`, `000004`, `000006`, `000007`, `000010`, `000011`.
- **Officer 2 (Colleague / Isolation Test):**
  - OID: `00000000-0000-0000-0000-000000000002`
  - Name: Mark Camilleri
  - Role: `ROLE_CLAIMS_OFFICER`
  - Access: Assigned claims `CLM-2026-000002`, `000005`, `000008`, `000009`, `000012`.
- **Case Isolation Behavior:** Switching to Officer 2 and attempting to view Officer 1's claim or download its documents returns HTTP 403 Forbidden, and Officer 1's claims are filtered out from Officer 2's dashboard.

---

## 6. Offline vs. Live Azure Capabilities

| Capability | Local / Offline Test Mode | Live Azure Cloud Mode |
|---|---|---|
| **Document Upload & Validation** | Local storage / MIME magic byte checks | Private Azure Blob Storage (`quarantine` / `clean` containers) |
| **Malware Scanning** | Automatic deterministic clearance | Microsoft Defender for Storage on-upload scanning via Event Grid |
| **Text Extraction (OCR)** | Deterministic regex / pattern extraction | Azure Document Intelligence (`prebuilt-layout`, 2024-11-30) |
| **Field Consolidation & Fact Verification** | In-memory rule engine | In-memory rule engine |
| **Tool Lookups (`policy`, `coverage`, `history`, `rules`)** | Spring Boot internal REST client to seeded DB | Spring Boot internal REST client to seeded DB |
| **Claim Summary Generation** | Deterministic citation-linked template | Azure OpenAI / Azure AI Foundry (`gpt-5.4-mini` / `gpt-4o-mini`) |
| **Evidence Verification & Guardrails** | Exact statement citation verification | Exact statement citation verification |
| **Officer Review & Annotation** | PostgreSQL table `officer_review` | PostgreSQL Flexible Server table `officer_review` |

---

## 7. Requirement-to-Evidence Matrix Summary

For the full detailed matrix with line citations, see [`docs/phase4-validation-and-evidence-matrix-2026-11-11.md`](docs/phase4-validation-and-evidence-matrix-2026-11-11.md).

- **FR-01 (Officer Case Intake):** Implemented in `ClaimController.java` & `claims-list-page.tsx`. Verified by `GroundedWorkflowIntegrationTest`.
- **FR-02 (Secure Multi-Format Upload):** Magic-byte verification in `FileMagicValidator.java`. Verified by `FileMagicValidatorTest`.
- **FR-03 (Anti-Malware Quarantine Gate):** Enforced in `DefenderScanResults.java`. Unscanned/quarantined files blocked from OCR.
- **FR-04 (OCR Text Extraction):** Document Intelligence wrapper with page/bounding boxes in `DocumentIntelligenceClientWrapper.java`.
- **FR-05 (Structured Field Extraction):** 100% extraction accuracy (`61/61` fields) without inventing missing data (`8/8` kept unknown).
- **FR-06 (Completeness Validation):** Rule validation in `FindingService.java` generates open findings for missing items.
- **FR-07 (Controlled Tool Lookups):** Scoped read-only tool calls (`get_policy`, `get_coverage`, `get_claim_history`, `get_business_rules`).
- **FR-08 (Cross-Validation Discrepancy Detection):** 100% precision & recall on policy discrepancies (`PolicyEvidenceValidator.java`).
- **FR-09 (Cited Summary Preparation):** 100% citation grounding rate (`133/133` statements backed by document/tool citations).
- **FR-10 (Explicit Uncertainty Reporting):** Missing fields and tool lookups returning `NOT_FOUND` explicitly documented in summary.
- **FR-11 (Automated Escalation Evaluation):** Escalates on prompt injection, discrepancies, or high damage thresholds (`EscalationEvaluator.java`).
- **FR-12 (Prompt-Injection Containment):** Heuristic detector flags injection attacks; run status set to `ESCALATED`; no unapproved actions taken.
- **FR-13 (Human Review & Annotation):** Immutable reviews and non-destructive statement corrections in `ReviewService.java`.
- **FR-14 (Audit Logging & Reproducibility):** SHA-256 digests of rules, prompts, inputs, and tool calls persisted in `AgentRun.java`.
- **FR-15 (Strict AI Decision Boundaries):** Review outcomes restricted to `ACKNOWLEDGED`, `ESCALATED`, `INFO_REQUESTED`.

---

## 8. Operational Handover, Recovery & Cloud Admin Blockers

### Operational Roles & Responsibilities
- **Product Owner / Business Sponsor:** MMAUG Group B Leadership.
- **Technical Lead / Backend Custodian:** Senior Claims Platform Engineer.
- **Data Protection Officer (DPO) / Key Custodian:** Responsible for the preserved 32-byte `PII_ENCRYPTION_KEY` in Azure Key Vault.

### Critical Safety Rule: Data & Key Preservation
> [!CAUTION]
> **NEVER generate a replacement PII encryption key** and **NEVER wipe or overwrite the production/staging database**. Existing encrypted records in PostgreSQL (`pg-mmaug-claims-stg-01`) require the original key stored in Key Vault (`kv-mmaug-claims-stg-01`). Any recovery or migration must use an isolated database and verify decryption before re-pointing connections.

### Cloud Staging Admin Action Items (Pending Subscription Admin Resolution)
1. **Azure Key Vault RBAC Grant:** Assign `Key Vault Secrets User` role on `kv-mmaug-claims-stg-01` to `id-mmaug-github-stg` and the App Service system-assigned managed identity (`app-mmaug-api-stg-01`) to resolve `AccessToKeyVaultDenied`.
2. **Defender for Storage Activation:** Enable on-upload scanning on storage account `stmmaugclaimsstg01` with a 1 GB/month cap and connect results to Event Grid topic `evgt-mmaug-malware-results-stg`.
3. **App Service Startup Alignment:** Ensure App Service environment variables point to the verified staging runtime parameters documented in [`docs/phase2-solution-design-and-iac-2026-11-11.md`](docs/phase2-solution-design-and-iac-2026-11-11.md).

---

## 9. Submission Archive Verification

A clean, source-only submission archive is assembled in the `output/` directory:
- **Archive File:** `MMAUG_Claims_Agent_Source_Submission_2026-11-11.zip`
- **Exclusions:** `.git/`, `node_modules/`, `target/`, `dist/`, `.cache/`, `tmp/`, `.env`, credentials, local logs, and database dumps.
- **Verification:** Includes complete source code, tests, Flyway migrations, seeds, Bicep IaC, OpenAPI contracts, and documentation.
