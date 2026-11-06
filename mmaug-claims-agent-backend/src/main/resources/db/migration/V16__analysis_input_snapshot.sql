ALTER TABLE agent_run ADD COLUMN input_snapshot JSONB;
ALTER TABLE agent_run ADD COLUMN input_snapshot_sha256 VARCHAR(64);
ALTER TABLE claim_document ADD COLUMN extraction_token UUID;
ALTER TABLE claim_document ADD COLUMN extraction_expires_at TIMESTAMPTZ;
CREATE INDEX idx_document_extraction_expiry ON claim_document(extraction_expires_at) WHERE processing_status='EXTRACTING';
-- Legacy in-flight document states have no owner token. Retire them through
-- the normal recovery sweep rather than silently leaving them EXTRACTING.
UPDATE claim_document SET extraction_token=gen_random_uuid(),extraction_expires_at=now(),version=version+1
WHERE processing_status='EXTRACTING' AND extraction_token IS NULL;
INSERT INTO extraction_result(id,document_id,status,extractor,prompt_version,schema_version)
SELECT extraction_token,id,'PENDING','LEGACY_RECOVERY','v1','v1'
FROM claim_document WHERE processing_status='EXTRACTING' AND extraction_token IS NOT NULL
ON CONFLICT(id) DO NOTHING;
