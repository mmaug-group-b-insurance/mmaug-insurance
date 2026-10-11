package com.mmaug.claimsagent.user;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Authentication",
        description = """
                Authentication is performed by Microsoft Entra ID. The frontend obtains a JWT
                via MSAL and sends it as `Authorization: Bearer <token>`. This controller exposes
                the authenticated user's identity and roles to the frontend after login.
                """)
@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    @Operation(
            summary = "Get current user profile",
            description = """
                    Returns the authenticated user's identity and roles, derived from the JWT.
                    The frontend calls this after sign-in to determine what the user can do.
                    The token itself is issued by Microsoft Entra ID, not by this backend.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<UserResponse> me(CurrentUser user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserResponse.builder()
                .id(parseUuidOrNull(user.userId()))
                .entraObjectId(user.entraOid())
                .displayName(null)
                .email(user.email())
                .roles(user.roles().stream().map(Enum::name).toList())
                .customerId(null)
                .build());
    }

    private java.util.UUID parseUuidOrNull(String s) {
        try {
            return java.util.UUID.fromString(s);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}