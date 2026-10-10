package com.mmaug.claimsagent.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Business rules view exposed to the internal tools API")
public class RulesView {

    private String ruleSetVersion;
    private List<String> requiredFields;
    private Thresholds thresholds;
    private List<EscalationRuleView> escalationRules;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Thresholds {
        private BigDecimal highValueAmount;
        private Integer lateReportingDays;
        private Integer maxClaimsPerYear;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EscalationRuleView {
        private String id;
        private String when;
        private String severity;
    }
}