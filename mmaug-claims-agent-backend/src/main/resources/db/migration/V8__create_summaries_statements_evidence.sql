-- V8: Summaries, statements, evidence

CREATE TABLE claim_summary (
                               id                     UUID PRIMARY KEY,
                               created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               version                BIGINT NOT NULL DEFAULT 0,
                               claim_id               UUID NOT NULL,
                               run_id                 UUID,
                               version_no             INT NOT NULL DEFAULT 1,
                               headline               VARCHAR(300),
                               overall_confidence     VARCHAR(10),
                               escalation_recommended BOOLEAN NOT NULL DEFAULT FALSE,
                               escalation_reasons     JSONB,
                               uncertainty_notes      JSONB,
                               llm_model              VARCHAR(60),
                               prompt_version         VARCHAR(20),
                               generated_at           TIMESTAMPTZ NOT NULL,
                               disclaimer             VARCHAR(500),
                               CONSTRAINT fk_claim_summary_claim FOREIGN KEY (claim_id) REFERENCES claim (id),
                               CONSTRAINT fk_claim_summary_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
);

CREATE TABLE summary_statement (
                                   id              UUID PRIMARY KEY,
                                   created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   version         BIGINT NOT NULL DEFAULT 0,
                                   summary_id      UUID NOT NULL,
                                   order_no        INT NOT NULL,
                                   section         VARCHAR(30) NOT NULL,
                                   statement_type  VARCHAR(20) NOT NULL,
                                   text            VARCHAR(1500),
                                   confidence      NUMERIC(4,3),
                                   CONSTRAINT fk_summary_statement_summary FOREIGN KEY (summary_id) REFERENCES claim_summary (id)
);

CREATE TABLE evidence_ref (
                              id            UUID PRIMARY KEY,
                              created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              version       BIGINT NOT NULL DEFAULT 0,
                              statement_id  UUID NOT NULL,
                              source_type   VARCHAR(15) NOT NULL,
                              document_id   UUID,
                              page_number   INT,
                              snippet       VARCHAR(500),
                              tool_call_id  UUID,
                              rule_id       VARCHAR(40),
                              field_name    VARCHAR(60),
                              CONSTRAINT fk_evidence_ref_statement FOREIGN KEY (statement_id) REFERENCES summary_statement (id)
);

CREATE INDEX idx_claim_summary_claim ON claim_summary (claim_id);
CREATE INDEX idx_claim_summary_version ON claim_summary (claim_id, version_no);
CREATE INDEX idx_summary_statement_summary ON summary_statement (summary_id);
CREATE INDEX idx_evidence_ref_statement ON evidence_ref (statement_id);