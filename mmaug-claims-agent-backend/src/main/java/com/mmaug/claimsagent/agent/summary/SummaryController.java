package com.mmaug.claimsagent.agent.summary;

import com.mmaug.claimsagent.agent.summary.dto.SummaryResponse;
import com.mmaug.claimsagent.agent.summary.dto.SummaryVersionItem;
import com.mmaug.claimsagent.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "AI Summaries", description = "AI-generated claim summaries with typed statements and evidence references")
@RestController
@RequestMapping("/api/v1/claims/{claimId}/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @Operation(
            summary = "Get claim summary",
            description = """
                    Returns the latest AI-generated claim summary.
                    The summary is decision-support only. It never approves, rejects,
                    determines liability, authorises payment, or modifies a policy.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary retrieved"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Summary not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<SummaryResponse> get(@PathVariable UUID claimId,
                                               @RequestParam(required = false) Integer version) {
        return ResponseEntity.ok(summaryService.getVersion(claimId, version));
    }

    @Operation(
            summary = "List summary versions",
            description = "Returns the list of summary versions available for the claim, newest first. "
                    + "Returns an empty list when no summaries exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions returned (may be empty)"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/versions")
    public ResponseEntity<List<SummaryVersionItem>> versions(@PathVariable UUID claimId) {
        return ResponseEntity.ok(summaryService.listVersions(claimId));
    }
}