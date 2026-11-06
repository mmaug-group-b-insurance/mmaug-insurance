package com.mmaug.claimsagent.agent.summary;

import java.math.BigDecimal;
import java.util.*;

/** A deterministic source quotation; values are data, never executable instructions. */
public record GroundedStatement(String section, StatementType type, String text, BigDecimal confidence,
                                List<Citation> citations) {
    public record Citation(SourceType sourceType, UUID documentId, UUID extractionId, Integer page,
                           String snippet, UUID toolCallId, String fieldName, String sourceValue) {}
    public static String documentText(String field, String value, boolean conflicting) {
        return (conflicting ? "Conflicting source value — " : "Document reports — ") + field + ": " + value;
    }
    public static String toolText(String tool, String field, String value) {
        return "Recorded lookup — " + tool + " / " + field + ": " + value;
    }
}
