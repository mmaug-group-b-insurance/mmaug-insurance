package com.mmaug.claimsagent.claim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Partial update for a draft claim")
public class UpdateClaimRequest {

    @Pattern(regexp = "^POL-MT-\\d{6}$", message = "must match POL-MT-######")
    private String policyNumber;

    @Size(max = 150)
    private String claimantName;

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "must be E.164 format")
    private String claimantPhone;

    @Email
    private String claimantEmail;

    @Size(max = 15)
    private String vehicleRegistration;

    @PastOrPresent
    private LocalDate incidentDate;

    private LocalTime incidentTime;

    @Size(max = 300)
    private String incidentLocation;

    @Size(min = 20, max = 5000)
    private String incidentDescription;

    private Boolean thirdPartyInvolved;

    @Size(max = 40)
    private String policeReportNumber;

    private Boolean injuriesReported;

    @PositiveOrZero
    private BigDecimal estimatedAmount;

    @Schema(description = "Optimistic locking version", example = "0")
    private Long version;
}