-- V5: Claim documents
-- Uploaded files are treated as untrusted input.
-- Files are stored in a quarantine container first, then scanned.

CREATE TABLE claim_document (
                                id                    UUID PRIMARY KEY,
                                created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                version               BIGINT NOT NULL DEFAULT 0,
                                claim_id              UUID NOT NULL,
                                original_filename     VARCHAR(255) NOT NULL,
                                blob_container        VARCHAR(40) NOT NULL,
                                blob_name             VARCHAR(200) NOT NULL,
                                content_type          VARCHAR(100) NOT NULL,
                                size_bytes            BIGINT NOT NULL,
                                sha256                VARCHAR(64) NOT NULL,
                                document_type         VARCHAR(25) NOT NULL,
                                detected_type         VARCHAR(25),
                                scan_status           VARCHAR(15) NOT NULL,
                                processing_status     VARCHAR(15) NOT NULL,
                                page_count            INT,
                                uploaded_by_user_id   UUID NOT NULL,
                                uploaded_at           TIMESTAMPTZ NOT NULL,
                                deleted_at            TIMESTAMPTZ,
                                retention_until       DATE,
                                CONSTRAINT fk_claim_document_claim FOREIGN KEY (claim_id) REFERENCES claim (id)
);

CREATE INDEX idx_claim_document_claim ON claim_document (claim_id);
CREATE INDEX idx_claim_document_scan_status ON claim_document (scan_status);
CREATE INDEX idx_claim_document_processing_status ON claim_document (processing_status);
CREATE INDEX idx_claim_document_deleted ON claim_document (deleted_at);