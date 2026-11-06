# Synthetic business-rule lookup increment — 4 November 2026

Supports BRD FR-08 and NFR-09. The Java/Spring Boot backend and frontend contracts are preserved.

## Implemented

- Analysis retrieves the existing code-defined synthetic motor rule set through a claim/run-bound, read-only tool call after policy lookup. The product is taken from that policy snapshot, never from model arguments.
- Only PRIVATE_MOTOR is supported. Other products return no match. Missing policy, denied access and unavailable lookup remain distinct; failed lookups do not fabricate rule data.
- Each successful run stores ruleSetVersion `synthetic-1.0.0`, SHA-256 of the serialized rule data and the full timestamped tool envelope. The run API exposes the version and hash.
- High-value and late-reporting escalation checks use the retrieved thresholds. If the rule snapshot is missing, these checks are skipped rather than silently substituting defaults. Independent injury, injection and unavailable-tool controls remain active.
- The authenticated internal business-rules API delegates to the same rule source as analysis.

## Limits

These are synthetic demonstration rules, not approved insurance rules. No YAML loading is claimed. Required-field validation, frequent-claimant evaluation and low-confidence handling are not fully driven by this snapshot. Policy discrepancy flags are separate checks. Coverage interpretation, final coverage eligibility, liability and claim decisions are outside this increment. Rules are recorded in the run; they do not become model-written factual statements.

## Verification

The isolated PostgreSQL workflow suite checks four recorded lookups, a version/hash on successful rule retrieval, missing-policy outcomes, source-linked summaries and immutable reviews. Unit tests cover unsupported products, policy/run binding, denied/unavailable outcomes and use of retrieved thresholds without fallback. Live Azure OCR/model calls are disabled during this suite.

Full suite result: **99 tests run, 97 passed, 0 failures/errors, 2 opt-in live tests skipped**.

Restart the backend from the PowerShell session holding its private Azure keys before local acceptance. Generate a fresh analysis and inspect `get_business_rules` in its tool calls, version/hash and escalation reasons. Existing summary versions remain unchanged.
