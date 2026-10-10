package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates structured LLM output against the claim-summary schema.
 * In production this can use a JSON Schema library; here it performs
 * structural checks that match the expected shape.
 */
@Component
public class OutputSchemaValidator {

    public List<String> validateSummaryShape(String headline,
                                             String overallConfidence,
                                             List<String> statementTexts) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        if (headline == null || headline.isBlank()) {
            errors.add("headline is required");
        }
        if (overallConfidence == null
                || !List.of("LOW", "MEDIUM", "HIGH").contains(overallConfidence)) {
            errors.add("overallConfidence must be LOW, MEDIUM or HIGH");
        }
        if (statementTexts == null || statementTexts.isEmpty()) {
            errors.add("at least one statement is required");
        }
        return errors;
    }
}