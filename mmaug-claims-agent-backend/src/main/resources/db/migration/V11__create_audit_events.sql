-- V11: Audit events (insert-only, hash-chained)

CREATE TABLE audit_event (
                             id              UUID PRIMARY KEY,
                             occurred_at     TIMESTAMPTZ NOT NULL,
                             actor_user_id   UUID,
                             actor_role      VARCHAR(30),
                             actor_type      VARCHAR(15),
                             action          VARCHAR(40) NOT NULL,
                             outcome         VARCHAR(10) NOT NULL,
                             entity_type     VARCHAR(30),
                             entity_id       UUID,
                             claim_id        UUID,
                             ip_address      VARCHAR(45),
                             user_agent      VARCHAR(255),
                             correlation_id  VARCHAR(64),
                             details         JSONB,
                             prev_hash       VARCHAR(64),
                             hash            VARCHAR(64)
);

CREATE INDEX idx_audit_event_occurred ON audit_event (occurred_at);
CREATE INDEX idx_audit_event_claim ON audit_event (claim_id);
CREATE INDEX idx_audit_event_actor ON audit_event (actor_user_id);
CREATE INDEX idx_audit_event_action ON audit_event (action);
CREATE INDEX idx_audit_event_correlation ON audit_event (correlation_id);

-- Insert-only enforcement at the database level.
-- The application role is assumed to be the same role that created the schema.
-- If a separate application role is used, replace PUBLIC with that role name.
REVOKE UPDATE, DELETE ON audit_event FROM PUBLIC;