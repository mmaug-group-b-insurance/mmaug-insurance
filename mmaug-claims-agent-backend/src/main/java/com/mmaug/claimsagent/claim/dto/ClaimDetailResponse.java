package com.mmaug.claimsagent.claim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detailed claim representation")
public class ClaimDetailResponse {

    private ClaimResponse claim;

    @Schema(description = "Open finding counts by severity")
    private Map<String, Integer> openFindingCounts;

    private UUID latestRunId;
    private UUID latestSummaryId;
}