package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimFact;
import com.mmaug.claimsagent.claim.FactStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContradictionDetector {

    public List<ValidationFinding> validate(Claim claim, List<ClaimFact> facts) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (ClaimFact fact : facts) {
            if (fact.getStatus() == FactStatus.CONFLICTING) {
                ValidationFinding f = new ValidationFinding();
                f.setClaim(claim);
                f.setFindingType(FindingType.CONTRADICTION);
                f.setSeverity(Severity.HIGH);
                f.setOrigin(FindingOrigin.RULE_ENGINE);
                f.setFieldName(fact.getFieldName());
                f.setMessage("Contradictory values detected for " + fact.getFieldName());
                f.setResolved(false);
                findings.add(f);
            }
        }
        return findings;
    }
}