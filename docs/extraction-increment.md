# US-04 extraction increment — 30 October 2026

Maps to BRD document understanding/provenance and US-04 (#30), with conflict preservation supporting US-05 (#31). Java/Spring Boot remains canonical; frontend files are unchanged.

## Delivered behaviour

- Extraction is refused before storage/OCR access for uncleared or deleted documents.
- Every extraction creates a separate result, structured JSON snapshot and private raw-text object. The extraction response exposes an additive `extractionId`; existing response fields remain.
- Deterministic line-label parsing preserves missing values as null, attaches document IDs and page numbers derived from form-feed page separators, and never borrows the next line's colon. Confidence 0.75 is a heuristic, not a measured probability.
- Consolidation now runs in the existing analysis flow for clean, extracted documents. It refuses cross-case evidence before writes. Conflicts retain the original value/source and competing value, extraction ID, document, page and snippet in `alternate_values`. Repeating the same conflict does not duplicate its alternative.
- `claim-facts.schema.json` documents the v1 DTO contract. It is not yet a runtime model-output validator.

## Verification

Run `scripts/Invoke-BackendChecks.ps1 -JdkHome <full-JDK-path> -Goal test` from the repository root. Docker is needed for the PostgreSQL application-context tests. Tests cover missing values, page/source provenance, instruction-like text, competing evidence, repeat consolidation, cross-case rejection, scan clearance and immutable extraction snapshots/raw-text object names.

Synthetic OCR text fixture: `mmaug-claims-agent-backend/src/test/resources/extraction/synthetic-motor-report.txt`. This is a provider-output fixture, not a supported upload format or a real customer record.

## Remaining acceptance work

Production PDF/image OCR is not delivered by the local UTF-8 stub. Only explicit page separators support page provenance in the deterministic parser; production OCR must supply reliable page boundaries. Repeated labels within a single document currently return the first occurrence. Extraction is label-based and does not cover all DTO fields.

Claim facts remain a mutable consolidated view; extraction snapshots preserve history, but run-specific fact snapshots, supersession/reconciliation of changed reprocessing, and concurrent consolidation require follow-up. Alternate sources are retained in persistence; frontend presentation needs separate interface agreement. Raw-text objects use the same private storage container as their cleared source; retention/purge integration and orphan cleanup after transaction failures remain work.

Validation, tools and evidence verification still contain orchestrator placeholders outside the consolidation step. This increment does not certify the complete analysis workflow. Do not close US-04 or US-05 solely from these unit tests. Next: version-aware consolidation and validation integration, then scoped tools and grounded summaries.

Verification result (30 October 2026): full Maven suite passed 28 tests, zero failures/errors/skips, including PostgreSQL application-context checks. git diff --check passed; frontend diff is empty. Changes remain local and uncommitted.
