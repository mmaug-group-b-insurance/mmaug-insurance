-- Preserve exact extraction provenance across later document reprocessing.
ALTER TABLE evidence_ref ADD COLUMN extraction_id UUID;
ALTER TABLE evidence_ref ADD CONSTRAINT fk_evidence_ref_extraction
    FOREIGN KEY (extraction_id) REFERENCES extraction_result(id);
CREATE INDEX idx_evidence_ref_extraction ON evidence_ref(extraction_id);
