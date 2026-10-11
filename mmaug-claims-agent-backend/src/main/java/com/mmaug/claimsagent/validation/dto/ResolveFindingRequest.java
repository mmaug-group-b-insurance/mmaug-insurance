package com.mmaug.claimsagent.validation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request to resolve a validation finding")
public class ResolveFindingRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Reason or note describing the resolution", example = "Confirmed with claimant by phone")
    private String resolutionNote;
}