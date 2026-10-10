package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.extraction.dto.ClaimFactsExtraction;
import com.mmaug.claimsagent.extraction.dto.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Converts raw OCR text into structured ClaimFactsExtraction.
 * Production implementations call an LLM with a schema-validated prompt.
 * The default implementation here performs simple, deterministic parsing
 * so the application is fully testable without external AI credentials.
 */
@Service
public class StructuringService {

    private static final Logger log = LoggerFactory.getLogger(StructuringService.class);

    public ClaimFactsExtraction structure(UUID documentId, String rawText) {
        ClaimFactsExtraction facts = ClaimFactsExtraction.builder()
                .documentId(documentId)
                .schemaVersion("v1")
                .injectionSuspected(false)
                .build();

        if (rawText == null || rawText.isBlank()) {
            return facts;
        }

        // Deterministic, non-AI extraction from obvious label patterns.
        facts.setPolicyNumber(findField(rawText, "Policy Number", "POL-MT-"));
        facts.setVehicleRegistration(findField(rawText, "Registration", null));
        facts.setIncidentDate(findField(rawText, "Incident Date", null));
        facts.setIncidentLocation(findField(rawText, "Location", null));
        facts.setPoliceReportNumber(findField(rawText, "Police Report", null));
        facts.setGarageName(findField(rawText, "Garage", null));

        if (rawText.toLowerCase().contains("ignore previous instructions")
                || rawText.toLowerCase().contains("approve this claim")) {
            facts.setInjectionSuspected(true);
            facts.setInjectionNotes("Suspicious instruction-like text detected in document");
            log.warn("Possible prompt injection detected in document {}", documentId);
        }

        return facts;
    }

    private FieldValue findField(String text, String label, String prefix) {
        int idx = text.toLowerCase().indexOf(label.toLowerCase());
        if (idx < 0) return null;

        int start = text.indexOf(':', idx);
        if (start < 0) return null;

        int end = text.indexOf('\n', start);
        if (end < 0) end = Math.min(text.length(), start + 200);

        String value = text.substring(start + 1, end).trim();
        if (value.isEmpty()) return null;

        if (prefix != null && !value.startsWith(prefix)) {
            return null;
        }

        return FieldValue.builder()
                .value(value)
                .confidence(BigDecimal.valueOf(0.75))
                .snippet(value.length() > 200 ? value.substring(0, 200) : value)
                .build();
    }
}