package com.mmaug.claimsagent.agent.guardrails;

import com.mmaug.claimsagent.claim.Claim;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class EscalationEvaluator {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(10000);
    private static final int LATE_REPORTING_DAYS = 14;

    public Evaluation evaluate(Claim claim, boolean injectionSuspected, boolean toolUnavailable) {
        return evaluate(claim, injectionSuspected, toolUnavailable, com.mmaug.claimsagent.tools.dto.RulesView.Thresholds.builder().highValueAmount(HIGH_VALUE_THRESHOLD).lateReportingDays(LATE_REPORTING_DAYS).build());
    }

    public Evaluation evaluate(Claim claim, boolean injectionSuspected, boolean toolUnavailable, com.mmaug.claimsagent.tools.dto.RulesView.Thresholds thresholds) {
        List<String> reasons = new ArrayList<>();
        boolean required = false;

        if (claim.getInjuriesReported() != null && claim.getInjuriesReported()) {
            reasons.add("Injuries reported");
            required = true;
        }
        if (thresholds != null && thresholds.getHighValueAmount() != null && claim.getEstimatedAmount() != null
                && claim.getEstimatedAmount().compareTo(thresholds.getHighValueAmount()) > 0) {
            reasons.add("Estimated amount exceeds high-value threshold");
            required = true;
        }
        if (thresholds != null && thresholds.getLateReportingDays() != null && claim.getIncidentDate() != null && claim.getReportedAt() != null) {
            long days = ChronoUnit.DAYS.between(claim.getIncidentDate(), claim.getReportedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());
            if (days > thresholds.getLateReportingDays()) {
                reasons.add("Late reporting (" + days + " days)");
                required = true;
            }
        }
        if (injectionSuspected) {
            reasons.add("Possible prompt injection detected");
            required = true;
        }
        if (toolUnavailable) {
            reasons.add("One or more tools were unavailable");
            required = true;
        }

        return new Evaluation(required, reasons);
    }

    public record Evaluation(boolean required, List<String> reasons) {}
}