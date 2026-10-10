package com.mmaug.claimsagent.agent.tools;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The explicit allow-list of read-only tools.
 * No other tool may be invoked by the agent.
 * Every tool here is GET-only and cannot write, approve, pay or modify policy.
 */
@Component
public class AgentToolRegistry {

    public static final Set<String> ALLOWED_TOOLS = Set.of(
            "get_policy",
            "get_coverage",
            "get_claim_history",
            "get_business_rules"
    );

    public boolean isAllowed(String toolName) {
        return ALLOWED_TOOLS.contains(toolName);
    }

    public Set<String> allowedTools() {
        return ALLOWED_TOOLS;
    }
}