# Live application OCR verification — 4 November 2026

Observed on the running local synthetic preview, not Azure staging.

- Backend health: UP at 127.0.0.1:8081; frontend 5174 and isolated PostgreSQL 55432 reachable.
- Generated synthetic PNG uploaded through POST /api/v1/claims/{id}/documents with explicit image/png MIME type.
- Seeded claim: CLM-2026-000001 (bd0cf1b3-60d5-4c2c-b7dd-c4fde0bab90d).
- Document: dd85c840-7edc-459a-98e1-580c501b407a, synthetic-ocr-acceptance-v2.png.
- Extraction: 0580b9f4-f955-4625-b9e1-18642f4ec6a9, SUCCEEDED, prebuilt-layout.
- Five recovered fields: policy POL-MT-100001, registration SYN123, incident date 2026-09-15, location Synthetic test location, description Synthetic front bumper damage. Each references page 1 and a matching snippet.
- Independent subsequent GET returned the same extraction ID and all five fields. Document list reports EXTRACTED, pageCount 1.
- Read-only PostgreSQL query confirmed the extraction row is SUCCEEDED with five associated extracted_field records.
- No frontend source change, deployment, credential read from user shell, or resource creation was required. The running backend used its own configured OCR credential.

## Limits and follow-up

This verifies HTTP upload -> real OCR -> deterministic structuring -> PostgreSQL persistence -> subsequent API retrieval for one synthetic image. Browser interaction and process-restart durability were not exercised. Fixed parser confidence 0.75 is a heuristic, not Azure OCR confidence or measured accuracy. LLM synthesis is not implemented by this flow. Local authentication bypass and no-op scanner remain development-only; this is not production or staging acceptance.

Initial PowerShell multipart upload defaulted to application/octet-stream; retained as document 1e22a867-03b3-483a-93f9-cf9be964ad31 in UPLOADED state and was not sent to OCR. A repeated identical file upload returned HTTP 500 against the existing SHA-256 uniqueness constraint. Follow-up: normalize validated MIME types and handle duplicate uploads explicitly with a safe documented response. No test documents were deleted.
