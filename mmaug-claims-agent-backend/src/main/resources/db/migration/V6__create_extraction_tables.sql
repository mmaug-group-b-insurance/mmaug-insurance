-- V6: Extraction tables
-- Raw OCR text is stored in blob storage, not here. This table stores metadata
-- and a JSON pointer to the structured extraction.

CREATE TABLE extraction_result (
                                   id                    UUID PRIMARY KEY,
                                   created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   version               BIGINT NOT NULL DEFAULT 0,
                                   document_id           UUID NOT NULL,
                                   status                VARCHAR(15) NOT NULL,
                                   extractor             VARCHAR(30),
                                   extractor_model       VARCHAR(60),
                                   llm_model             VARCHAR(60),
                                   prompt_version        VARCHAR(20),
                                   schema_version        VARCHAR(20),
                                   raw_text_blob_name    VARCHAR(200),
                                   structured_json       JSONB,
                                   overall_confidence    NUMERIC(4,3),
                                   injection_suspected   BOOLEAN NOT NULL DEFAULT FALSE,
                                   error_code            VARCHAR(40),
                                   error_message         VARCHAR(500),
                                   CONSTRAINT fk_extraction_result_document FOREIGN KEY (document_id) REFERENCES claim_document (id)
);

CREATE TABLE extracted_field (
                                 id                UUID PRIMARY KEY,
                                 created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 version           BIGINT NOT NULL DEFAULT 0,
                                 extraction_id     UUID NOT NULL,
                                 document_id       UUID NOT NULL,
                                 field_name        VARCHAR(60) NOT NULL,
                                 field_value       VARCHAR(1000),
                                 normalised_value  VARCHAR(1000),
                                 confidence        NUMERIC(4,3),
                                 page_number       INT,
                                 bounding_box      JSONB,
                                 snippet           VARCHAR(500),
                                 CONSTRAINT fk_extracted_field_extraction FOREIGN KEY (extraction_id) REFERENCES extraction_result (id),
                                 CONSTRAINT fk_extracted_field_document FOREIGN KEY (document_id) REFERENCES claim_document (id)
);

CREATE INDEX idx_extraction_result_document ON extraction_result (document_id);
CREATE INDEX idx_extraction_result_status ON extraction_result (status);
CREATE INDEX idx_extracted_field_extraction ON extracted_field (extraction_id);
CREATE INDEX idx_extracted_field_document ON extracted_field (document_id);
CREATE INDEX idx_extracted_field_name ON extracted_field (field_name);