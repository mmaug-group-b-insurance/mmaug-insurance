package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimFact;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfidenceChecker {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(0.75);

    public List<ValidationFinding> validate(Claim claim, List<ClaimFact> facts) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (ClaimFact fact : facts) {
            if (fact.getConfidence() == null || fact.getConfidence().compareTo(THRESHOLD) < 0) {
                ValidationFinding f = new ValidationFinding();
                f.setClaim(claim);
                f.setFindingType(FindingType.LOW_CONFIDENCE);
                f.setSeverity(Severity.LOW);
                f.setOrigin(FindingOrigin.RULE_ENGINE);
                f.setFieldName(fact.getFieldName());
                f.setMessage("Low confidence for field: " + fact.getFieldName());
                f.setResolved(false);
                findings.add(f);
            }
        }
        return findings;
    }
}