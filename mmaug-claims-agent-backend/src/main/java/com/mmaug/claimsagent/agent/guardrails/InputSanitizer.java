package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

/**
 * Sanitises untrusted document text before it enters any LLM context.
 * The document content is treated as DATA, never as instructions.
 */
@Component
public class InputSanitizer {

    private static final int MAX_LENGTH = 60_000;

    public String sanitize(String raw) {
        if (raw == null) return "";
        String cleaned = raw
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .trim();
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }
        return cleaned;
    }

    public String asDataBlock(String raw) {
        return "<DATA>\n" + sanitize(raw) + "\n</DATA>";
    }
}