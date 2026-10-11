package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.claim.Claim;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that all required fields are present on the claim.
 * Rules are loaded from business-rules.yaml in later phases; for now they are
 * implemented inline and match business-rules.yaml version 1.0.0.
 */
@Component
public class CompletenessValidator {

    public List<ValidationFinding> validate(Claim claim) {
        List<ValidationFinding> findings = new ArrayList<>();

        requireField(findings, claim, "policyNumber", claim.getPolicyNumberStated());
        requireField(findings, claim, "claimantName", claim.getClaimantName());
        requireField(findings, claim, "vehicleRegistration", claim.getVehicleRegistration());
        requireField(findings, claim, "incidentDate",
                claim.getIncidentDate() != null ? claim.getIncidentDate().toString() : null);
        requireField(findings, claim, "incidentLocation", claim.getIncidentLocation());
        requireField(findings, claim, "incidentDescription", claim.getIncidentDescription());

        if (Boolean.TRUE.equals(claim.getThirdPartyInvolved())) {
            // Third-party details are typically captured during extraction; flag if missing altogether.
            // We cannot inspect ClaimFact here without a repository, so this is a placeholder.
        }

        if (Boolean.TRUE.equals(claim.getInjuriesReported()) && isBlank(claim.getPoliceReportNumber())) {
            findings.add(finding(claim, FindingType.MISSING_FIELD, Severity.HIGH,
                    "policeReportNumber",
                    "Injuries reported but no police report number supplied"));
        }

        return findings;
    }

    private void requireField(List<ValidationFinding> findings, Claim claim, String name, String value) {
        if (isBlank(value)) {
            findings.add(finding(claim, FindingType.MISSING_FIELD, Severity.MEDIUM,
                    name, "Required field missing: " + name));
        }
    }

    private ValidationFinding finding(Claim claim, FindingType type, Severity severity,
                                      String field, String message) {
        ValidationFinding f = new ValidationFinding();
        f.setClaim(claim);
        f.setFindingType(type);
        f.setSeverity(severity);
        f.setOrigin(FindingOrigin.RULE_ENGINE);
        f.setFieldName(field);
        f.setMessage(message);
        f.setResolved(false);
        return f;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}