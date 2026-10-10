package com.mmaug.claimsagent.agent;

public enum AgentRunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    ESCALATED,
    FAILED
}