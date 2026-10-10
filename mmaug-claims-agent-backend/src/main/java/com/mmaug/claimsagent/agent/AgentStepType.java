package com.mmaug.claimsagent.agent;

public enum AgentStepType {
    SCAN_CHECK,
    OCR,
    STRUCTURE,
    CONSOLIDATE,
    VALIDATE,
    TOOL_CALL,
    GUARDRAIL,
    GENERATE_SUMMARY,
    VERIFY_EVIDENCE,
    ESCALATION_CHECK
}