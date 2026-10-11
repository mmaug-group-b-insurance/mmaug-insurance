package com.mmaug.claimsagent.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Officer review record")
public class ReviewResponse {

    private UUID id;
    private UUID claimId;
    private UUID summaryId;
    private UUID reviewer;
    private String outcome;
    private String comment;
    private String infoRequested;
    private Boolean agreedWithEscalation;
    private List<EditItem> edits;
    private Instant reviewedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Recorded statement-level edit")
    public static class EditItem {
        private UUID statementId;
        private String originalText;
        private String editedText;
        private String reason;
    }
}