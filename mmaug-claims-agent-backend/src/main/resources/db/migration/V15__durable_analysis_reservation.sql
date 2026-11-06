-- A committed reservation survives rollback of the processing transaction.
ALTER TABLE agent_run ADD COLUMN retry_of_run_id UUID REFERENCES agent_run(id);
ALTER TABLE agent_run ADD COLUMN expires_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uq_agent_run_active_claim ON agent_run(claim_id) WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX idx_agent_run_active_expiry ON agent_run(expires_at) WHERE status IN ('QUEUED','RUNNING');

-- Aliases bind every keyed request (including duplicates of an active run).
CREATE TABLE agent_run_request (
 claim_id UUID NOT NULL REFERENCES claim(id),
 actor_id UUID NOT NULL,
 request_key VARCHAR(100) NOT NULL,
 run_id UUID NOT NULL REFERENCES agent_run(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(claim_id,actor_id,request_key)
);
CREATE INDEX idx_agent_run_request_run ON agent_run_request(run_id);
