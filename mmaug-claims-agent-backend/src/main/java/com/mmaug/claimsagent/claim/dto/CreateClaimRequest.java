package com.mmaug.claimsagent.claim.dto;

import com.mmaug.claimsagent.claim.ClaimChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request used to create an insurance claim")
public class CreateClaimRequest {

    @NotBlank
    @Pattern(regexp = "^POL-MT-\\d{6}$", message = "must match POL-MT-######")
    @Schema(description = "Insurance policy number", example = "POL-MT-123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String policyNumber;

    @NotBlank
    @Size(max = 150)
    @Schema(description = "Claimant full name", example = "Maria Borg")
    private String claimantName;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "must be E.164 format")
    @Schema(description = "Claimant phone in E.164 format", example = "+35679123456")
    private String claimantPhone;

    @NotBlank
    @Email
    @Schema(description = "Claimant email address", example = "claimant@example.com")
    private String claimantEmail;

    @NotBlank
    @Size(max = 15)
    @Schema(description = "Vehicle registration", example = "ABC 123")
    private String vehicleRegistration;

    @NotNull
    @PastOrPresent
    @Schema(description = "Date the incident occurred", example = "2026-09-15")
    private LocalDate incidentDate;

    @Schema(description = "Time the incident occurred", example = "14:30:00")
    private LocalTime incidentTime;

    @NotBlank
    @Size(max = 300)
    @Schema(description = "Location of the incident", example = "Triq Manwel Dimech, Sliema")
    private String incidentLocation;

    @NotBlank
    @Size(min = 20, max = 5000)
    @Schema(description = "Detailed description of the incident",
            example = "Vehicle was involved in a road traffic accident at the junction...")
    private String incidentDescription;

    @NotNull
    @Schema(description = "Whether a third party was involved")
    private Boolean thirdPartyInvolved;

    @Size(max = 40)
    @Schema(description = "Police report reference number (optional)")
    private String policeReportNumber;

    @NotNull
    @Schema(description = "Whether injuries were reported")
    private Boolean injuriesReported;

    @PositiveOrZero
    @Schema(description = "Estimated claim amount (optional)", example = "1250000.00")
    private BigDecimal estimatedAmount;

    @Schema(description = "Channel through which the claim is submitted",
            example = "WEB_PORTAL")
    private ClaimChannel channel;
}