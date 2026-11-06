package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimFact;
import com.mmaug.claimsagent.claim.ClaimFactRepository;
import com.mmaug.claimsagent.claim.FactStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class FactConsolidator {

    private final ExtractedFieldRepository fieldRepository;
    private final ClaimFactRepository claimFactRepository;

    public FactConsolidator(ExtractedFieldRepository fieldRepository,
                            ClaimFactRepository claimFactRepository) {
        this.fieldRepository = fieldRepository;
        this.claimFactRepository = claimFactRepository;
    }

    /**
     * Merge per-document extracted fields into one ClaimFact per field name.
     * When multiple documents disagree, status becomes CONFLICTING.
     */
    public void consolidate(Claim claim, UUID extractionId) {
        List<ExtractedField> fields = fieldRepository.findAllByExtractionId(extractionId);
        Map<String, ClaimFact> existing = new HashMap<>();
        for (ClaimFact fact : claimFactRepository.findAllByClaimId(claim.getId())) {
            existing.put(fact.getFieldName(), fact);
        }

        for (ExtractedField field : fields) {
            if (!claim.getId().equals(field.getDocument().getClaim().getId())) {
                throw new IllegalArgumentException("Extraction belongs to another claim");
            }
        }
        for (ExtractedField field : fields) {
            if (field.getFieldValue() == null || field.getFieldValue().isBlank()) continue;
            ClaimFact fact = existing.get(field.getFieldName());
            if (fact == null) {
                fact = new ClaimFact();
                fact.setClaim(claim);
                fact.setFieldName(field.getFieldName());
                fact.setValueText(field.getNormalisedValue() != null ? field.getNormalisedValue() : field.getFieldValue());
                fact.setConfidence(field.getConfidence() != null ? field.getConfidence() : BigDecimal.ZERO);
                fact.setSourceType("DOCUMENT");
                fact.setSourceDocumentId(field.getDocument().getId());
                fact.setSourcePage(field.getPageNumber());
                fact.setSourceSnippet(field.getSnippet());
                fact.setStatus(determineStatus(field.getConfidence()));
                claimFactRepository.save(fact);
                existing.put(field.getFieldName(), fact);
            } else {
                // Compare with existing
                String newValue = field.getNormalisedValue() != null ? field.getNormalisedValue() : field.getFieldValue();
                if (newValue != null && !newValue.equalsIgnoreCase(fact.getValueText())) {
                    var mapper = JsonMapper.builder().build();
                    var alternatives = fact.getAlternateValues() == null
                            ? mapper.createArrayNode()
                            : (tools.jackson.databind.node.ArrayNode) mapper.readTree(fact.getAlternateValues());
                    var source = mapper.createObjectNode();
                    source.put("value", newValue);
                    source.put("documentId", field.getDocument().getId().toString());
                    source.put("extractionId", extractionId.toString());
                    if (field.getPageNumber() != null) source.put("page", field.getPageNumber());
                    source.put("snippet", field.getSnippet());
                    boolean recorded = false;
                    for (var alternative : alternatives) {
                        if (alternative.equals(source)) recorded = true;
                    }
                    if (!recorded) alternatives.add(source);
                    fact.setAlternateValues(alternatives.toString());
                    fact.setStatus(FactStatus.CONFLICTING);
                    claimFactRepository.save(fact);
                }
            }
        }
    }

    private FactStatus determineStatus(BigDecimal confidence) {
        if (confidence == null) return FactStatus.LOW_CONFIDENCE;
        return confidence.compareTo(BigDecimal.valueOf(0.75)) < 0
                ? FactStatus.LOW_CONFIDENCE
                : FactStatus.CONFIRMED;
    }
}