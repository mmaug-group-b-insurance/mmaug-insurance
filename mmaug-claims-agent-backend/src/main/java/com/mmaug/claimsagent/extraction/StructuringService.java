package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.extraction.dto.ClaimFactsExtraction;
import com.mmaug.claimsagent.extraction.dto.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;

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
        facts.setPolicyholderName(findField(rawText, "Policyholder Name", null));
        facts.setDriverName(findField(rawText, "Driver Name", null));
        facts.setDamageDescription(findField(rawText, "Damage Description", null));
        facts.setEstimatedRepairCost(findField(rawText, "Estimated Repair Cost", null));
        facts.setCurrency(findField(rawText, "Currency", null));
        // Capture the source value; policy-format/lookup validation belongs downstream.
        facts.setPolicyNumber(findField(rawText, "Policy Number", null));
        facts.setVehicleRegistration(findField(rawText, "Registration", null));
        if (facts.getVehicleRegistration() == null) {
            facts.setVehicleRegistration(findField(rawText, "Vehicle Registration", null));
        }
        facts.setIncidentDate(findField(rawText, "Incident Date", null));
        facts.setIncidentLocation(findField(rawText, "Location", null));
        facts.setPoliceReportNumber(findField(rawText, "Police Report", null));
        facts.setGarageName(findField(rawText, "Garage", null));

        if (rawText.toLowerCase(Locale.ROOT).contains("ignore previous instructions")
                || rawText.toLowerCase(Locale.ROOT).contains("approve this claim")) {
            facts.setInjectionSuspected(true);
            facts.setInjectionNotes("Suspicious instruction-like text detected in document");
            log.warn("Possible prompt injection detected in document {}", documentId);
        }

        java.util.stream.Stream.of(facts.getPolicyNumber(), facts.getPolicyholderName(),
                facts.getDriverName(), facts.getVehicleRegistration(), facts.getIncidentDate(),
                facts.getIncidentLocation(), facts.getPoliceReportNumber(), facts.getGarageName(),
                facts.getDamageDescription(), facts.getEstimatedRepairCost(), facts.getCurrency())
                .filter(java.util.Objects::nonNull)
                .forEach(value -> value.setSourceDocumentId(documentId));
        return facts;
    }

    private FieldValue findField(String text, String label, String prefix) {
        // Anchor labels to a line: never borrow a colon/value from another field.
        var matcher = Pattern.compile("(?im)(?:^|(?<=\\f))\\h*" + Pattern.quote(label)
                + "\\h*:\\h*([^\\r\\n\\f]*)").matcher(text);
        if (!matcher.find()) return null;
        String value = matcher.group(1).trim();
        if (value.isEmpty() || value.length() > 1000) return null;
        if (prefix != null && !value.startsWith(prefix)) return null;
        int page = 1;
        for (int i = 0; i < matcher.start(1); i++) {
            if (text.charAt(i) == '\f') page++;
        }

        return FieldValue.builder()
                .value(value)
                .page(page)
                .confidence(BigDecimal.valueOf(0.75))
                .snippet(value.length() > 200 ? value.substring(0, 200) : value)
                .build();
    }
}
