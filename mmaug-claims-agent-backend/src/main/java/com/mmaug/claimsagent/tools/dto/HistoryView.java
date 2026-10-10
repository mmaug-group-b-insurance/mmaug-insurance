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
@Schema(description = "Claim history view exposed to the internal tools API")
public class HistoryView {

    private String policyNumber;
    private Integer totalClaims;
    private BigDecimal totalPaid;
    private List<HistoryItem> claims;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryItem {
        private String claimReference;
        private LocalDate lossDate;
        private String lossType;
        private BigDecimal amountPaid;
        private String outcome;
        private Boolean atFault;
    }
}