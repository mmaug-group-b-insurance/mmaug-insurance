# Phase 5 — Final Handover & Official Submission Package (11 November 2026)

## 1. Submission Overview

- **Project:** MMAUG Insurance Claims Preparation Agent (Group B)
- **Customer:** MMAUG Insurance Ltd, Malta
- **Participant RFP:** `MMAUG-AIDB-RFP-003`
- **Candidate Git Commit:** `df3f512` on branch `release/friday-submission` (merged in worktree `feature/officer-intake-scope`)
- **Official Portal Target:** [MMAUG Bootcamp Capstone Submission Portal](https://mmaug.com/bootcamp/capstone-submit)
- **Submission Source Archive:**
  - File: `MMAUG_Claims_Agent_Source_Submission_2026-11-11.zip`
  - Locations:
    - `c:\Users\fumeh\OneDrive\Documents\ChatGPT\MMAUG - Insurance Claims Agent\output\MMAUG_Claims_Agent_Source_Submission_2026-11-11.zip`
    - `c:\Users\fumeh\Downloads\MMAUG - Insurance Claims Agent\MMAUG_Claims_Agent_Source_Submission_2026-11-11.zip`
  - Size: **764,924 bytes (747 KB)**
  - SHA-256 Checksum: `680EBEDAEDB4A3E4CD89583F483480799A3D8FBC527831E884AD916CE5CEC2B6`

---

## 2. Summary of Completed Phases (1 through 5)

| Phase | Delivered Scope | Key Evidence & Artifacts | Commit |
|---|---|---|---|
| **Phase 1 · Discovery, Case Isolation & Catalogue** | Fixed 20-policy (`POL-MT-100001`–`100020`), 20-customer (`CUS-0001`–`0020`), and 12-claim catalogue (`4 Complete`, `4 Incomplete`, `4 Conflicting`). Enforced Entra v2 OIDC delegated scope (`scp=mmaug_insurance_app`), valid `oid`, and per-officer case isolation (`ClaimAccessPolicy.java`, `claims-list-page.tsx`). | [`docs/phase1-scope-and-catalogue-2026-11-11.md`](phase1-scope-and-catalogue-2026-11-11.md), `R__seed_*.sql`, `live-cases.json` | `e385102` |
| **Phase 2 · Solution Design & Declarative IaC** | Created declarative Azure Bicep IaC (`infra/staging/main.bicep` & `.bicepparam`), validated with Bicep CLI v0.49.2 (`exit 0`). Documented 22-resource Azure footprint, as-built vs proposed reconciliation, D01–D07 architectural decisions, and dependency recovery paths. | [`docs/phase2-solution-design-and-iac-2026-11-11.md`](phase2-solution-design-and-iac-2026-11-11.md), `observed-inventory-2026-11-11.json`, `main.bicep` | `0e0b64a` |
| **Phase 3 · Build & Integration Verification** | Built executable Spring Boot JAR (`target/mmaug-claims-agent-backend-0.0.1-SNAPSHOT.jar`, SHA-256: `73798D...`) and production React SPA (`frontend/dist/`, 0 audit vulnerabilities). Ran `Test-ReleaseJar.ps1 -RestoreSyntheticSnapshot` (health 200, unauth 401, CORS 200/403) and `Test-SyntheticDatabaseRestore.ps1` (isolated target restore, source unchanged). | [`docs/phase3-release-verification-2026-11-11.json`](phase3-release-verification-2026-11-11.json), release bundle `phase3-candidate-20261111-100847` | `e92e8ca` |
| **Phase 4 · Validation, 12-Case Scorecard & Matrix** | Added 12-case end-to-end evaluation suite + adversarial prompt injection test (`GroundedWorkflowIntegrationTest.java`). Full backend suite: **160 tests run, 0 failures, 2 skipped**. Frontend suite: **35/35 passed**. Evaluated: 100% field accuracy (61/61), 100% omission recall (8/8), 100% conflict precision/recall (TP=6, FP=0, FN=0), 100% citation grounding (133/133), max latency 0.140s. | [`docs/phase4-validation-results-2026-11-11.json`](phase4-validation-results-2026-11-11.json), [`docs/phase4-validation-and-evidence-matrix-2026-11-11.md`](phase4-validation-and-evidence-matrix-2026-11-11.md) | `c3f42cb` |
| **Phase 5 · Handover README, Main Repo Sync & Clean ZIP** | Replaced placeholder `README.md` with full architecture, offline/cloud quick start, 12-scenario catalogue guide, officer setup, and operational runbook. Switched main repo to `release/friday-submission`. Produced clean source-only submission ZIP stripped of git, binaries, node_modules, and secrets. | [`README.md`](../README.md), submission archive `.zip` & SHA-256 | `df3f512` |

---

## 3. Quick Verification Commands for Assessor

To verify the submission from a clean machine:
```powershell
# 1. Unpack ZIP
Expand-Archive -Path "MMAUG_Claims_Agent_Source_Submission_2026-11-11.zip" -DestinationPath "mmaug-submission"
cd "mmaug-submission"

# 2. Run Backend Tests (160 tests, 0 failures)
.\scripts\Invoke-GroundedSprintChecks.ps1 -JdkHome "$env:JAVA_HOME"

# 3. Run Frontend Tests (35 tests, 0 failures)
cd frontend
npm ci
npm test -- --run
npm run typecheck

# 4. Verify Release Smoke & Synthetic Restore Integrity
cd ..
.\scripts\Test-ReleaseJar.ps1 -RestoreSyntheticSnapshot
.\scripts\Test-SyntheticDatabaseRestore.ps1
```

---

## 4. Operational Sign-off & Known Cloud Status

- **Offline Demonstration Status:** **100% operational and green**. All functional intake, document validation, anti-malware quarantine checks, deterministic OCR, tool lookups, grounded summaries, officer review pinning, and case isolation are fully functional without external cloud dependencies.
- **Azure Staging Cloud Status:** Host infrastructure is active in `rg_mmaug_insurance`. Two admin action items remain with the Azure/Entra subscription owner:
  1. Grant `Key Vault Secrets User` role on `kv-mmaug-claims-stg-01` to `id-mmaug-github-stg` and `app-mmaug-api-stg-01` to resolve `AccessToKeyVaultDenied`.
  2. Enable on-upload scanning on `stmmaugclaimsstg01` with a 1 GB/month cap and point findings to Event Grid topic `evgt-mmaug-malware-results-stg`.
- **Safety Directive:** Existing encrypted data in `pg-mmaug-claims-stg-01` and its corresponding 32-byte `PII_ENCRYPTION_KEY` must be preserved—never generate a replacement key or wipe staging data.
