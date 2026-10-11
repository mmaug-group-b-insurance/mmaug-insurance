package com.mmaug.claimsagent.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authenticated user profile")
public class UserResponse {

    @Schema(description = "Local user ID")
    private UUID id;

    @Schema(description = "Entra Object ID (oid claim)")
    private String entraObjectId;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Application roles")
    private List<String> roles;

    @Schema(description = "Linked customer ID (claimants only)")
    private UUID customerId;
}