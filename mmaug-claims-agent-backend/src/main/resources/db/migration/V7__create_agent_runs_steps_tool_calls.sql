-- V7: Agent runs, steps, tool calls

CREATE TABLE agent_run (
                           id                     UUID PRIMARY KEY,
                           created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           version                BIGINT NOT NULL DEFAULT 0,
                           claim_id               UUID NOT NULL,
                           triggered_by_user_id   UUID NOT NULL,
                           status                 VARCHAR(25) NOT NULL,
                           correlation_id         VARCHAR(64),
                           prompt_version         VARCHAR(20),
                           tool_schema_version    VARCHAR(20),
                           rule_set_version       VARCHAR(20),
                           rule_set_sha256        VARCHAR(64),
                           llm_model              VARCHAR(60),
                           llm_parameters         JSONB,
                           started_at             TIMESTAMPTZ,
                           completed_at           TIMESTAMPTZ,
                           tokens_in              INT,
                           tokens_out             INT,
                           escalation_required    BOOLEAN NOT NULL DEFAULT FALSE,
                           warnings               JSONB,
                           error_code             VARCHAR(40),
                           error_message          VARCHAR(500),
                           CONSTRAINT fk_agent_run_claim FOREIGN KEY (claim_id) REFERENCES claim (id)
);

CREATE TABLE agent_step (
                            id              UUID PRIMARY KEY,
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            version         BIGINT NOT NULL DEFAULT 0,
                            run_id          UUID NOT NULL,
                            step_no         INT NOT NULL,
                            step_type       VARCHAR(20) NOT NULL,
                            name            VARCHAR(80) NOT NULL,
                            status          VARCHAR(15) NOT NULL,
                            input_summary   JSONB,
                            output_summary  JSONB,
                            input_hash      VARCHAR(64),
                            duration_ms     INT,
                            started_at      TIMESTAMPTZ,
                            error_code      VARCHAR(40),
                            CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
);

CREATE TABLE tool_call (
                           id                    UUID PRIMARY KEY,
                           created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           version               BIGINT NOT NULL DEFAULT 0,
                           run_id                UUID NOT NULL,
                           step_id               UUID,
                           tool_name             VARCHAR(60) NOT NULL,
                           tool_schema_version   VARCHAR(20),
                           arguments_json        JSONB,
                           result_json           JSONB,
                           status                VARCHAR(15) NOT NULL,
                           http_status           INT,
                           latency_ms            INT,
                           retry_count           INT DEFAULT 0,
                           error_code            VARCHAR(40),
                           error_message         VARCHAR(500),
                           called_at             TIMESTAMPTZ NOT NULL,
                           CONSTRAINT fk_tool_call_run FOREIGN KEY (run_id) REFERENCES agent_run (id),
                           CONSTRAINT fk_tool_call_step FOREIGN KEY (step_id) REFERENCES agent_step (id)
);

CREATE INDEX idx_agent_run_claim ON agent_run (claim_id);
CREATE INDEX idx_agent_run_status ON agent_run (status);
CREATE INDEX idx_agent_step_run ON agent_step (run_id);
CREATE INDEX idx_agent_step_step_no ON agent_step (run_id, step_no);
CREATE INDEX idx_tool_call_run ON tool_call (run_id);
CREATE INDEX idx_tool_call_tool_name ON tool_call (tool_name);