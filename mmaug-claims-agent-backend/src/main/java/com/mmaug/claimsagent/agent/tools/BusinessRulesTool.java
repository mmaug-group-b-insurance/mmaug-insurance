package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.tools.dto.RulesView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BusinessRulesTool {

    public RulesView load(String productCode) {
        if (!"PRIVATE_MOTOR".equals(productCode)) throw new com.mmaug.claimsagent.common.exception.NotFoundException("No synthetic rules for product");
        return RulesView.builder()
                .ruleSetVersion("synthetic-1.0.0")
                .requiredFields(List.of(
                        "policyNumber", "claimantName", "vehicleRegistration",
                        "incidentDate", "incidentLocation", "incidentDescription"))
                .thresholds(RulesView.Thresholds.builder()
                        .highValueAmount(BigDecimal.valueOf(10000))
                        .lateReportingDays(14)
                        .maxClaimsPerYear(2)
                        .build())
                .escalationRules(List.of(
                        RulesView.EscalationRuleView.builder().id("ESC-001").when("policyNotActiveOnIncidentDate").severity("HIGH").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-002").when("estimatedAmount > highValueAmount").severity("HIGH").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-003").when("injuriesReported == true").severity("HIGH").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-004").when("claimsInLastYear > maxClaimsPerYear").severity("MEDIUM").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-005").when("reportedAt - incidentDate > lateReportingDays").severity("MEDIUM").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-006").when("injectionSuspected == true").severity("CRITICAL").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-007").when("toolUnavailable == true").severity("MEDIUM").build(),
                        RulesView.EscalationRuleView.builder().id("ESC-008").when("overallConfidence == LOW").severity("MEDIUM").build()))
                .build();
    }
}