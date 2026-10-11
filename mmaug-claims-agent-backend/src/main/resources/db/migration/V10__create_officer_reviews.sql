-- V10: Officer reviews
-- No APPROVED or REJECTED outcome exists. The final claim decision is made outside this system.

CREATE TABLE officer_review (
                                id                     UUID PRIMARY KEY,
                                created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                version                BIGINT NOT NULL DEFAULT 0,
                                claim_id               UUID NOT NULL,
                                summary_id             UUID,
                                reviewer_id            UUID NOT NULL,
                                outcome                VARCHAR(30) NOT NULL,
                                comment                TEXT,
                                info_requested         TEXT,
                                agreed_with_escalation BOOLEAN,
                                reviewed_at            TIMESTAMPTZ NOT NULL,
                                CONSTRAINT fk_officer_review_claim FOREIGN KEY (claim_id) REFERENCES claim (id),
                                CONSTRAINT fk_officer_review_summary FOREIGN KEY (summary_id) REFERENCES claim_summary (id)
);

CREATE TABLE officer_edit (
                              id              UUID PRIMARY KEY,
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              version         BIGINT NOT NULL DEFAULT 0,
                              review_id       UUID NOT NULL,
                              statement_id    UUID NOT NULL,
                              original_text   VARCHAR(1500),
                              edited_text     VARCHAR(1500),
                              reason          VARCHAR(500),
                              CONSTRAINT fk_officer_edit_review FOREIGN KEY (review_id) REFERENCES officer_review (id),
                              CONSTRAINT fk_officer_edit_statement FOREIGN KEY (statement_id) REFERENCES summary_statement (id)
);

CREATE INDEX idx_officer_review_claim ON officer_review (claim_id);
CREATE INDEX idx_officer_review_outcome ON officer_review (outcome);
CREATE INDEX idx_officer_edit_review ON officer_edit (review_id);
CREATE INDEX idx_officer_edit_statement ON officer_edit (statement_id);