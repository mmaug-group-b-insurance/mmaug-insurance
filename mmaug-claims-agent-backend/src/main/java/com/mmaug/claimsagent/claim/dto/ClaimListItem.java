package com.mmaug.claimsagent.claim.dto;

import com.mmaug.claimsagent.claim.ClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Claim list item")
public class ClaimListItem {

    private UUID id;
    private String claimNumber;
    private ClaimStatus status;
    private String claimantName;
    private String policyNumberStated;
    private LocalDate incidentDate;
    private Boolean riskFlag;
    private Integer documentCount;
    private Integer openFindingCount;
    private Instant createdAt;
}