package com.mmaug.claimsagent.dashboard;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.dashboard.dto.DashboardResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Role-aware dashboard aggregates for the React frontend")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(
            summary = "Get dashboard aggregates",
            description = """
                    Returns claim status counts, claims awaiting review, escalated claims,
                    risk-flagged claims, open findings, and recent activity.
                    The scope depends on the authenticated user's role:
                    claimants see only their own claims; officers, auditors and admins see all.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard data returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<DashboardResponse> get(CurrentUser user) {
        return ResponseEntity.ok(dashboardService.build(user));
    }
}