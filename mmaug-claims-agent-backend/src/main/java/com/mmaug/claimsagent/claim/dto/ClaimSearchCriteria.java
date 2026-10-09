package com.mmaug.claimsagent.claim.dto;

import com.mmaug.claimsagent.claim.ClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Search filters for claims")
public class ClaimSearchCriteria {

    private ClaimStatus status;
    private String claimNumber;
    private String policyNumber;
    private String claimantName;
    private Boolean riskFlag;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Boolean assignedToMe;

    @Schema(description = "Zero-based page index", example = "0")
    private Integer page = 0;

    @Schema(description = "Page size (max 50)", example = "20")
    private Integer size = 20;

    @Schema(description = "Sort field and direction, e.g. createdAt,desc", example = "createdAt,desc")
    private String sort = "createdAt,desc";
}