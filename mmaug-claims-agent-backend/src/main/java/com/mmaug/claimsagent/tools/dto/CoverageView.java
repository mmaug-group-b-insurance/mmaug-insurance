package com.mmaug.claimsagent.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Coverage view exposed to the internal tools API")
public class CoverageView {

    private String policyNumber;
    private List<CoverageItem> coverages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoverageItem {
        private String coverageType;
        private Boolean included;
        private BigDecimal limitAmount;
        private BigDecimal excessAmount;
        private String exclusionsText;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
    }
}