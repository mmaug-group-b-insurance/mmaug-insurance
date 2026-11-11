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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Authentication",
        description = """
                Authentication is performed by Microsoft Entra ID. The frontend obtains a JWT
                via MSAL and sends it as `Authorization: Bearer <token>`. This controller exposes
                the authenticated user's identity and roles to the frontend after login.
                Roles are managed in the application database.
                """)
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = """
                    Returns the authenticated user's identity and roles, derived from the database
                    and JWT. The frontend calls this after sign-in to determine what the user can do.
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

        Optional<AppUser> dbUserOpt = userService.findByEntraObjectId(user.entraOid());
        String displayName = dbUserOpt.map(AppUser::getDisplayName).orElse(null);
        UUID customerId = dbUserOpt.map(AppUser::getCustomerId).orElse(null);
        UUID userId = dbUserOpt.map(AppUser::getId).orElseGet(() -> parseUuidOrNull(user.userId()));
        List<String> roles = user.roles().stream().map(Enum::name).toList();

        return ResponseEntity.ok(UserResponse.builder()
                .id(userId)
                .entraObjectId(user.entraOid())
                .displayName(displayName)
                .email(user.email())
                .roles(roles)
                .customerId(customerId)
                .build());
    }

    private UUID parseUuidOrNull(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
