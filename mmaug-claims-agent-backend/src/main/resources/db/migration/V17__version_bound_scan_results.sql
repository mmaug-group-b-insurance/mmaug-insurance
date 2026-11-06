ALTER TABLE claim_document ADD COLUMN storage_etag VARCHAR(150);
ALTER TABLE claim_document ADD COLUMN scan_event_id VARCHAR(150);
ALTER TABLE claim_document ADD COLUMN scan_finished_at TIMESTAMPTZ;
ALTER TABLE claim_document ADD COLUMN scan_result_code VARCHAR(40);
CREATE INDEX ix_document_blob_scan ON claim_document(blob_container, blob_name) WHERE deleted_at IS NULL;
-- Existing local evidence stays unchanged. Azure uploads require a new, pinned blob ETag.
