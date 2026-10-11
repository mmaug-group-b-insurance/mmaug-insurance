package com.mmaug.claimsagent.review;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.review.dto.CreateReviewRequest;
import com.mmaug.claimsagent.review.dto.ReviewResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reviews", description = """
        Human review workflow. The AI never approves or rejects a claim.
        Officers acknowledge, request information, escalate, accept for investigation,
        or reject the summary for regeneration. The final decision is made outside the system.
        """)
@RestController
@RequestMapping("/api/v1/claims/{claimId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "Submit officer review",
            description = """
                    Submits a human review. Outcomes are ACKNOWLEDGED, INFO_REQUESTED,
                    ESCALATED_TO_SENIOR, ACCEPTED_FOR_INVESTIGATION, or SUMMARY_REJECTED_REGENERATE.
                    There is no approve or reject outcome.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review submitted"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only claims officers may submit reviews",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim or summary not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Missing required field for outcome",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> create(@PathVariable UUID claimId,
                                                 @Valid @RequestBody CreateReviewRequest request,
                                                 CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(claimId, request, user));
    }

    @Operation(summary = "List reviews for a claim")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews listed"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> list(@PathVariable UUID claimId, CurrentUser user) {
        return ResponseEntity.ok(reviewService.list(claimId, user));
    }
}