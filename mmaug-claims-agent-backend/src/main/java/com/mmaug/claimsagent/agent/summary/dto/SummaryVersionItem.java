package com.mmaug.claimsagent.agent.summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One entry in the summary version history")
public class SummaryVersionItem {

    private UUID summaryId;
    private Integer versionNo;
    private Instant generatedAt;
    private String headline;
    private String overallConfidence;
}