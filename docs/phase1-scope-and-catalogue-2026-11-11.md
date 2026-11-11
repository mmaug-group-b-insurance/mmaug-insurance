# Phase 1 Scope Freeze & 12-Case Catalogue — 11 November 2026

## 1. Frozen MVP Scope & Governance Decisions (FR-01 to FR-12)

- **Primary Actor & Boundary:** Authenticated internal `CLAIMS_OFFICER` preparing synthetic motor accidental-damage cases (`PRIVATE_MOTOR`, plus `FLEET` seed policies for negative/unsupported product rule verification). The system never approves or rejects claims, determines liability, authorises payment, or modifies policy records.
- **Identity & Object-Level Access (`FR-01`):**
  - Authenticated requests require the Entra v2 delegated scope (`scp` containing `mmaug_insurance_app`) and a valid UUID directory object ID (`oid`).
  - Frontend resolves active roles from `/api/v1/me` rather than unverified client token inspection.
  - A `CLAIMS_OFFICER` can list, view, upload documents to, extract, analyse, or review **only** cases where `assigned_officer_id` (or `created_by_user_id` for legacy unassigned rows) matches their own `oid` (`ClaimAccessPolicy`). `AUDITOR` and `ADMIN` retain read-all visibility for oversight.
- **Intake vs. Evidence Completeness (`FR-02`, `FR-04`, `FR-05`):**
  - Officer intake captures stated claim metadata and binds `assigned_officer_id` to the creating officer.
  - Completeness validation (`CompletenessValidator`) checks required preparation fields (`policyNumber`, `claimantName`, `vehicleRegistration`, `incidentDate`, `incidentLocation`, `incidentDescription`, plus `policeReportNumber` when `injuriesReported = true`) and flags missing fields without fabricating values.
- **Private Uploads & Quarantine (`FR-03`):**
  - Permitted file formats: PDF, JPG/JPEG, PNG, TIFF (verified by extension and magic-byte header signature; maximum 15 MB per file, up to 20 files per claim).
  - Files enter `quarantine` (`ScanStatus.PENDING`) and cannot be downloaded or extracted until cleared (`ScanStatus.CLEAN`).
- **Conflict Preservation & Controlled Tools (`FR-06`–`FR-08`):**
  - Cross-document conflicts (`FactConsolidator` / `ContradictionDetector`) and evidence-vs-policy discrepancies (`PolicyEvidenceValidator`: `CONTRADICTION`, `VEHICLE_MISMATCH`, `POLICY_INACTIVE_ON_DATE`) preserve both values and their source citations and trigger human escalation.
  - Read-only case-bound tools (`get_policy`, `get_coverage`, `get_claim_history`, `get_business_rules`) record immutable snapshots and distinguish `OK`, `NOT_FOUND`, `DENIED`, and `UNAVAILABLE`.
- **Grounded Summary & Exact-Version Acknowledgement (`FR-09`–`FR-12`):**
  - Every published statement in `ClaimSummary` must pass `EvidenceVerifier` against stored document extractions or run-scoped tool snapshots.
  - Officer review (`ReviewService`) requires the latest `summaryId`, records actor/timestamp/outcome, preserves original machine statements alongside any officer edits, and rejects cross-claim or stale-version submissions.

---

## 2. Canonical Synthetic Dataset & 12-Case Catalogue (BRD §15)

The repeatable seed suite (`src/main/resources/db/seed/`) provisions **20 synthetic customers (`CUS-0001`–`CUS-0020`)**, **20 synthetic policies (`POL-MT-100001`–`POL-MT-100020`)** with vehicles, drivers, coverages, and 9 historical claims (`HIST-2022`–`HIST-2026`), plus **12 distinct seeded claims (`CLM-2026-000001`–`CLM-2026-000012`)** split **4 Complete / 4 Incomplete / 4 Conflicting** across two officer identities (`00000000-0000-0000-0000-000000000001` and `00000000-0000-0000-0000-000000000002`).

| Seeded Claim | Category | Assigned Officer | Policy & Vehicle | Scenario & Expected Outcome |
|---|---|---|---|---|
| `CLM-2026-000001` | Complete (1/4) | Officer 1 (`...0001`) | `POL-MT-100001` (`ABC 123`) | Active comprehensive policy; all required fields present; clean lookup and cited summary. |
| `CLM-2026-000002` | Complete (2/4) | Officer 2 (`...0002`) | `POL-MT-100002` (`DEF 456`) | Active policy on incident date (`2026-02-20`); isolated to Officer 2 (`403` for Officer 1). |
| `CLM-2026-000004` | Complete (3/4) | Officer 1 (`...0001`) | `POL-MT-100006` (`MNO 345`) | Active policy; complete intake and PDF/PNG evidence match (`complete-pdf-claim-form`). |
| `CLM-2026-000005` | Complete (4/4) | Officer 2 (`...0002`) | `POL-MT-100008` (`STU 901`) | Active policy with courtesy car coverage; compact scan fixture (`complete-compact-scan`). |
| `CLM-2026-000006` | Incomplete (1/4) | Officer 1 (`...0001`) | `POL-MT-100007` (`NULL`) | Missing `vehicleRegistration` and `incidentLocation`; flagged by `CompletenessValidator`. |
| `CLM-2026-000007` | Incomplete (2/4) | Officer 1 (`...0001`) | `POL-MT-100011` (`BCD 890`) | `injuriesReported = true` with missing `policeReportNumber`; high-severity `MISSING_FIELD` + escalation. |
| `CLM-2026-000008` | Incomplete (3/4) | Officer 1 (`...0001`) | `POL-MT-100013` (`HIJ 246`) | Draft phone intake missing `incidentDescription` and claimant contact details. |
| `CLM-2026-000009` | Incomplete (4/4) | Officer 2 (`...0002`) | `POL-MT-100016` (`NULL`) | Broker intake missing `vehicleRegistration`; isolated to Officer 2. |
| `CLM-2026-000003` | Conflicting (1/4) | Officer 1 (`...0001`) | `POL-MT-100001` (`ABC 123`) | Multi-discrepancy / high-risk collision (`three-discrepancies` evaluation fixture: policy, vehicle, and date flags). |
| `CLM-2026-000010` | Conflicting (2/4) | Officer 1 (`...0001`) | `POL-MT-100003` (`GHI 789`) | Lapsed policy (`2023-06-01` to `2024-05-31`) vs. `2026-10-05` incident date (`POLICY_INACTIVE_ON_DATE`). |
| `CLM-2026-000011` | Conflicting (3/4) | Officer 1 (`...0001`) | `POL-MT-100009` (`ZZ 9999`) | Vehicle registration mismatch (`ZZ 9999` vs. policy `VWX 234` -> `VEHICLE_MISMATCH`) + 3 prior claims in 2026. |
| `CLM-2026-000012` | Conflicting (4/4) | Officer 2 (`...0002`) | `POL-MT-100010` (`YZA 567`) | High-value estimate (`14,500 EUR` > `10,000 EUR` threshold), late reporting, and policy number discrepancy fixture (`CONTRADICTION`). |

All 12 evaluation fixtures (including PNG, PDF, and smaller-font/reordered layouts) are defined in `tests/evaluation/live-cases.json` and validated by `scripts/evaluate_live_claims.py`.
