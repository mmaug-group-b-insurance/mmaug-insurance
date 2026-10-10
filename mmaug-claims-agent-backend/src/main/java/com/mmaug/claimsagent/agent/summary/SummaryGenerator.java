package com.mmaug.claimsagent.agent.summary;

import com.mmaug.claimsagent.agent.guardrails.ForbiddenClaimsFilter;
import com.mmaug.claimsagent.agent.guardrails.OutputSchemaValidator;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimFact;
import com.mmaug.claimsagent.claim.ClaimFactRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a structured ClaimSummary from claim facts.
 * The default implementation is deterministic and does not call an external LLM.
 * Production replaces this with a provider-backed implementation behind the
 * ClaimAnalysisService abstraction (master prompt section 31).
 */
@Service
public class SummaryGenerator {

    private final ClaimFactRepository claimFactRepository;
    private final ForbiddenClaimsFilter forbiddenFilter;
    private final OutputSchemaValidator schemaValidator;

    public SummaryGenerator(ClaimFactRepository claimFactRepository,
                            ForbiddenClaimsFilter forbiddenFilter,
                            OutputSchemaValidator schemaValidator) {
        this.claimFactRepository = claimFactRepository;
        this.forbiddenFilter = forbiddenFilter;
        this.schemaValidator = schemaValidator;
    }

    public GeneratedSummary generate(Claim claim) {
        List<ClaimFact> facts = claimFactRepository.findAllByClaimId(claim.getId());

        String headline = buildHeadline(claim, facts);
        String overallConfidence = computeOverallConfidence(facts);

        List<GeneratedStatement> statements = new ArrayList<>();
        statements.add(new GeneratedStatement("OVERVIEW", StatementType.EXTRACTED_FACT,
                "Claim " + claim.getClaimNumber() + " concerns " + safe(claim.getIncidentDescription(), 200),
                BigDecimal.valueOf(0.8)));

        for (ClaimFact fact : facts) {
            String text = humanise(fact.getFieldName()) + ": " + fact.getValueText();
            StatementType type = switch (fact.getStatus()) {
                case CONFIRMED -> StatementType.EXTRACTED_FACT;
                case CONFLICTING -> StatementType.UNCERTAIN;
                case MISSING -> StatementType.RECOMMENDED_CHECK;
                case LOW_CONFIDENCE -> StatementType.UNCERTAIN;
            };
            statements.add(new GeneratedStatement("INCIDENT", type, text,
                    fact.getConfidence() != null ? fact.getConfidence() : BigDecimal.valueOf(0.5)));
        }

        // Guardrail: strip forbidden wording
        statements.removeIf(s -> forbiddenFilter.containsForbidden(s.text()));

        List<String> uncertaintyNotes = new ArrayList<>();
        for (ClaimFact fact : facts) {
            if (fact.getStatus() == com.mmaug.claimsagent.claim.FactStatus.CONFLICTING) {
                uncertaintyNotes.add("Conflicting value for " + fact.getFieldName());
            } else if (fact.getStatus() == com.mmaug.claimsagent.claim.FactStatus.MISSING) {
                uncertaintyNotes.add("Missing value for " + fact.getFieldName());
            }
        }

        schemaValidator.validateSummaryShape(headline, overallConfidence,
                statements.stream().map(GeneratedStatement::text).toList());

        return new GeneratedSummary(headline, overallConfidence, statements, uncertaintyNotes);
    }

    private String buildHeadline(Claim claim, List<ClaimFact> facts) {
        return "Decision-support summary for claim " + claim.getClaimNumber()
                + " (" + facts.size() + " facts consolidated)";
    }

    private String computeOverallConfidence(List<ClaimFact> facts) {
        if (facts.isEmpty()) return "LOW";
        double sum = 0;
        int count = 0;
        for (ClaimFact fact : facts) {
            if (fact.getConfidence() != null) {
                sum += fact.getConfidence().doubleValue();
                count++;
            }
        }
        if (count == 0) return "LOW";
        double avg = sum / count;
        if (avg >= 0.85) return "HIGH";
        if (avg >= 0.6) return "MEDIUM";
        return "LOW";
    }

    private String humanise(String fieldName) {
        if (fieldName == null) return "";
        String spaced = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String safe(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    public record GeneratedSummary(
            String headline,
            String overallConfidence,
            List<GeneratedStatement> statements,
            List<String> uncertaintyNotes
    ) {}

    public record GeneratedStatement(
            String section,
            StatementType type,
            String text,
            BigDecimal confidence
    ) {}
}