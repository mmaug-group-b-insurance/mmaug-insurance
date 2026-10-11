package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.validation.dto.FindingResponse;
import com.mmaug.claimsagent.validation.dto.ResolveFindingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Findings", description = "Validation findings: missing, contradictory, low-confidence")
@RestController
@RequestMapping("/api/v1")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @Operation(summary = "List findings for a claim",
            description = "Lists validation findings filtered by type, severity and resolution status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Findings listed"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/claims/{claimId}/findings")
    public ResponseEntity<List<FindingResponse>> list(
            @PathVariable UUID claimId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved,
            CurrentUser user) {
        return ResponseEntity.ok(findingService.list(claimId, type, severity, resolved, user));
    }

    @Operation(summary = "Resolve finding",
            description = "Marks a finding as resolved with a resolution note. Only claims officers may resolve findings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finding resolved"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Finding not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/findings/{findingId}/resolve")
    public ResponseEntity<FindingResponse> resolve(@PathVariable UUID findingId,
                                                   @Valid @RequestBody ResolveFindingRequest request,
                                                   CurrentUser user) {
        return ResponseEntity.ok(findingService.resolve(findingId, request, user));
    }
}