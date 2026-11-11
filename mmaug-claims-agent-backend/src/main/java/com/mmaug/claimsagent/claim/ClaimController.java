package com.mmaug.claimsagent.claim;

import com.mmaug.claimsagent.claim.dto.ClaimDetailResponse;
import com.mmaug.claimsagent.claim.dto.ClaimListItem;
import com.mmaug.claimsagent.claim.dto.ClaimResponse;
import com.mmaug.claimsagent.claim.dto.ClaimSearchCriteria;
import com.mmaug.claimsagent.claim.dto.CreateClaimRequest;
import com.mmaug.claimsagent.claim.dto.UpdateClaimRequest;
import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Claims", description = "Claim creation, retrieval, update, submission and management APIs")
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "Create claim", description = "Creates a new draft claim for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody CreateClaimRequest request,
                                                CurrentUser user) {
        ClaimResponse response = claimService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get claim by ID", description = "Retrieves a claim and its accessible claim information for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access this claim",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimDetailResponse> get(@PathVariable UUID claimId, CurrentUser user) {
        return ResponseEntity.ok(claimService.getDetail(claimId, user));
    }

    @Operation(summary = "List claims", description = "Paginated list of claims filtered by the supplied criteria and the caller's role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claims retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<Page<ClaimListItem>> list(ClaimSearchCriteria criteria, CurrentUser user) {
        return ResponseEntity.ok(claimService.search(criteria, user));
    }

    @Operation(summary = "Update draft claim", description = "Partially updates a draft claim. Officers can edit their assigned claims.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Not permitted"),
            @ApiResponse(responseCode = "404", description = "Claim not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent modification detected")
    })
    @PatchMapping("/{claimId}")
    public ResponseEntity<ClaimResponse> update(@PathVariable UUID claimId,
                                                @Valid @RequestBody UpdateClaimRequest request,
                                                CurrentUser user) {
        return ResponseEntity.ok(claimService.update(claimId, request, user));
    }

    @Operation(summary = "Submit claim", description = "Transitions a draft claim to SUBMITTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim submitted"),
            @ApiResponse(responseCode = "403", description = "Not permitted"),
            @ApiResponse(responseCode = "404", description = "Claim not found"),
            @ApiResponse(responseCode = "422", description = "Claim is not in DRAFT status")
    })
    @PostMapping("/{claimId}/submit")
    public ResponseEntity<ClaimResponse> submit(@PathVariable UUID claimId, CurrentUser user) {
        return ResponseEntity.ok(claimService.submit(claimId, user));
    }
}