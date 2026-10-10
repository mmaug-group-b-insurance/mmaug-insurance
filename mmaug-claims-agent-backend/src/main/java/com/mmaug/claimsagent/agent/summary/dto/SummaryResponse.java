package com.mmaug.claimsagent.agent.summary.dto;

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
@Schema(description = "AI-generated claim summary with typed statements and evidence")
public class SummaryResponse {

    private UUID summaryId;
    private UUID claimId;
    private Integer versionNo;
    private String headline;
    private String overallConfidence;
    private Boolean escalationRecommended;
    private List<String> escalationReasons;
    private List<String> uncertaintyNotes;
    private String disclaimer;
    private Instant generatedAt;
    private String promptVersion;
    private String llmModel;

    @Schema(description = "Statements grouped by section")
    private Map<String, List<StatementResponse>> sections;
}