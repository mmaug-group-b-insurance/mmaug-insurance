# Live synthetic evaluation increment — 5 November 2026

The local Java backend completed four new cases using Azure Document Intelligence prebuilt-layout and Azure OpenAI. Existing demonstration summaries and human reviews were not regenerated. No frontend source or production behaviour changed. No officer acknowledgements or finding resolutions were submitted by the evaluation runner.

## Scope and acceptance evidence

This increment supports FR-04 (extraction), FR-06 (conflicts), FR-09 (grounded summaries), NFR-06 (explainability) and NFR-09 (repeatable regression checks). It samples missing evidence relevant to FR-05; it does not validate the complete business-required-field rule set.

The labels in `tests/evaluation/live-cases.json` were fixed before these live calls. They are developer-authored synthetic fixtures, not independent holdout evidence. Inputs are rendered PNGs with known text, including a smaller-font/reordered variant. Scanned PDFs, handwriting, multipage documents, poor scans and representative real-world layout diversity are not covered.

| Case | Preview claim | Result |
| --- | --- | --- |
| Matching policy | CLM-2026-000006 | Five expected values matched; no policy discrepancy flags |
| Three discrepancies | CLM-2026-000007 | Five values matched; policy/vehicle/date discrepancies all retained |
| Missing date and location | CLM-2026-000008 | Three values matched; two omitted fields stayed unknown |
| Smaller type, reordered fields | CLM-2026-000009 | Five values matched; no policy discrepancy flags |

Machine-readable results, run/claim/summary IDs, hashes and provider token counts are in `live-evaluation-results-2026-11-05.json`. Full local synthetic response snapshots and rendered inputs are under `C:\Users\fumeh\.cache\mmaug-preview\evaluation-20261105-a`.

## Metrics and interpretation

- Present-field exact string matches: **18/18**. No fuzzy matching or post-run label adjustment.
- Explicitly omitted fields kept unknown: **2/2**; reported separately from present-field accuracy.
- Policy discrepancy detection: **3 true positives, 0 false positives, 0 false negatives**, scoring sets per case for CONTRADICTION, VEHICLE_MISMATCH and POLICY_INACTIVE_ON_DATE. Other business findings are outside this metric.
- Every published statement had a citation. Document citations matched the case's extraction ID, document ID, field value and page; tool citations referenced the same run's saved tool calls. This checks reference integrity, not source truth or unrestricted semantic entailment.
- Five evaluator tests passed, including wrong-value, fabricated-missing-field, false-positive/negative, missing/incorrect-citation and zero-denominator cases.

All four live cases passed these checks. The small dataset's perfect observed scores do not establish a general accuracy estimate, calibrated confidence, fraud/coverage correctness, business sign-off or the proposed BRD target. The model organizes constrained backend-grounded statements. The displayed 0.75 field confidence remains uncalibrated. Policy/history/rules use local synthetic data and local preview identity/scanning remains in place.

## Repeatable execution

Requires Python and Pillow, a running local preview with provider credentials already configured securely in its backend process, and the synthetic lookup policy POL-MT-100001. The runner never reads credentials.

From the repository root:

```powershell
python -m unittest discover -s tests/evaluation -p test_scoring.py
python scripts/evaluate_live_claims.py --output "$env:USERPROFILE\.cache\mmaug-preview\evaluation-new"
# Explicit live execution creates four new cases and incurs OCR/model usage:
python scripts/evaluate_live_claims.py --output "$env:USERPROFILE\.cache\mmaug-preview\evaluation-new" --execute-live
```

Use a fresh output directory for each live run. Without `--execute-live`, the script loads the manifest and makes no API calls. The live path only supports localhost, records each case incrementally, exits nonzero for scored failures and stops on service errors without automatic paid retries. Maximum normal invocation: four analyses, each with one generated document. Model/schema/grounding safety still runs in the backend. Provider costs are not estimated by token counts alone.

## Review-form clarification

The existing frontend displays the escalation-agreement checkbox only for ESCALATED_TO_SENIOR. ACKNOWLEDGED initializes and submits `agreedWithEscalation=false`, including when that checkbox is hidden. Therefore the earlier acknowledgement's false value is not evidence of lost input or explicit disagreement. Treat the outcome as acknowledgement; do not infer the officer's escalation stance from that default. No historical review was changed. If the team wants a distinct unspecified stance, agree a nullable/API/UI change with the frontend owner first.

## Next acceptance work

1. A business reviewer should inspect these four summaries and their original images and record suitability, omissions and wording concerns; a machine reference check cannot substitute for this.
2. Obtain a separately labelled, varied synthetic holdout set from another reviewer. Agree fields, normalization, missing-field denominator, discrepancy matching, sample size and acceptance thresholds before running it. Keep regression and holdout scores separate.
3. Validate the required-field rules, thresholds and policy/history semantics with the team. Existing numerical rules remain synthetic proposals.
4. Complete team review/CI and authenticated staging gates separately: real Entra access, malware scanning/private storage, restore/deployment acceptance and the USD 100/month staging plan. No VPN Gateway is included.

The increment is local and uncommitted; no Azure resources were created or deployed.
