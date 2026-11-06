# Upload hardening — 4 November 2026

Follows live OCR acceptance findings. No frontend source or database schema changes.

- FileValidator returns the canonical MIME type from its validated extension/signature pair. DocumentService persists and stores this type instead of trusting the multipart Content-Type. Generic octet-stream PNG/JPEG/PDF/TIFF inputs can therefore reach OCR with the correct supported media type. Existing rows are not rewritten. Signature checks are intake checks, not a substitute for malware scanning or full-format validation.
- Fixed .tiff mapping to the existing TIFF magic-byte checks.
- Upload takes a PostgreSQL pessimistic write lock on the claim inside the existing transaction, serializing duplicate/count checks between uploads to the same claim.
- Active same-claim SHA-256 duplicate is rejected before blob storage or scanning. API responds 409 DUPLICATE_DOCUMENT using the existing ApiError envelope. The existing partial unique index remains the database safeguard. Other claims and deleted rows remain outside this duplicate query.
- No changes to extraction confidence, LLM behavior, authentication or scanning.

Focused tests cover generic/incorrect multipart MIME headers, TIFF mapping, duplicate rejection without storage/scanning side effects, 409 mapping, existing validation negatives, and the upload/extraction flow. Mock-based tests do not prove PostgreSQL lock concurrency; that remains an integration acceptance check.

Restart the Java backend to activate the changes; the currently running backend does not hot-load these classes. Repeat the same synthetic image upload to CLM-2026-000001 and expect 409 with a clear message, then upload new synthetic image bytes with generic multipart MIME and verify persisted contentType=image/png. No additional paid OCR calls are needed to verify these two upload cases. Live acceptance of this change has not yet been run.

## Live acceptance after backend restart

4 November 2026: verified via local application API against PostgreSQL.

- Identical existing synthetic image rejected with HTTP 409, code DUPLICATE_DOCUMENT, clear safe message.
- New synthetic PNG uploaded using PowerShell multipart default generic MIME; returned persisted contentType=image/png, scanStatus=CLEAN and processingStatus=UPLOADED. Document f460d5d2-9d5c-49d2-92bb-6e8e03b94280, synthetic-mime-check.png.
- Two concurrent HTTP uploads of identical new synthetic image bytes with explicit application/octet-stream produced one HTTP 201 and one HTTP 409. Subsequent document list contained exactly one matching active document, with contentType=image/png. This exercises the per-claim transaction lock/duplicate path with real PostgreSQL; broader load testing remains outstanding.
- No OCR calls made for these checks. New MIME/concurrency test documents remain UPLOADED and have no extraction result. Use synthetic-ocr-acceptance-v2.png for the previously verified extraction display.
