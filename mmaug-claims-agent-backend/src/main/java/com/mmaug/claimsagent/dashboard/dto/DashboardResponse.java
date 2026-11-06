package com.mmaug.claimsagent.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard aggregate data, tailored to the authenticated user's role")
public class DashboardResponse {

    @Schema(description = "Claim counts by status")
    private Map<String, Long> statusCounts;

    @Schema(description = "Claims awaiting human review")
    private Long awaitingReviewCount;

    @Schema(description = "Claims currently escalated")
    private Long escalatedCount;

    @Schema(description = "Claims flagged with a risk")
    private Long riskFlaggedCount;

    @Schema(description = "Open findings from each accessible claim's current run (unbound findings only before its first successful run)")
    private Long openFindingsCount;

    @Schema(description = "Recent activity for the authenticated user's scope")
    private List<ActivityItem> recentActivity;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Recent activity item")
    public static class ActivityItem {
        private UUID claimId;
        private String claimNumber;
        private String event;
        private Instant occurredAt;
    }
}