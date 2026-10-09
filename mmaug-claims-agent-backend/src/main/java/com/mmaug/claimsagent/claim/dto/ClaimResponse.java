package com.mmaug.claimsagent.claim.dto;

import com.mmaug.claimsagent.claim.ClaimChannel;
import com.mmaug.claimsagent.claim.ClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Claim representation")
public class ClaimResponse {

    private UUID id;
    private String claimNumber;
    private ClaimStatus status;
    private ClaimChannel channel;
    private String policyNumberStated;
    private UUID policyId;
    private String claimantName;
    private String claimantPhone;
    private String claimantEmail;
    private String vehicleRegistration;
    private LocalDate incidentDate;
    private LocalTime incidentTime;
    private String incidentLocation;
    private String incidentDescription;
    private Boolean thirdPartyInvolved;
    private String policeReportNumber;
    private Boolean injuriesReported;
    private BigDecimal estimatedAmount;
    private String currency;
    private Boolean riskFlag;
    private String escalationReason;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant updatedAt;
    private Long version;
}