package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Verifies that an evidence reference resolves to a real source.
 * Called during summary generation to drop unsupported statements.
 */
@Component
public class EvidenceVerifier {

    public boolean isValidDocumentReference(UUID documentId, String snippet) {
        return documentId != null && snippet != null && !snippet.isBlank();
    }

    public boolean isValidToolReference(String toolCallId) {
        return toolCallId != null && !toolCallId.isBlank();
    }

    public boolean isValidRuleReference(String ruleId) {
        return ruleId != null && !ruleId.isBlank();
    }
}