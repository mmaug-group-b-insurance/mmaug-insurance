package com.mmaug.claimsagent.review.dto;

import com.mmaug.claimsagent.review.ReviewOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Officer review submission. No APPROVED or REJECTED outcome exists.")
public class CreateReviewRequest {

    @NotNull
    @Schema(description = "Summary version reviewed", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID summaryId;

    @NotNull
    @Schema(description = "Review outcome",
            example = "ACCEPTED_FOR_INVESTIGATION",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private ReviewOutcome outcome;

    @NotBlank
    @Size(max = 4000)
    @Schema(description = "Reviewer comment")
    private String comment;

    @Size(max = 4000)
    @Schema(description = "Information requested (required when outcome is INFO_REQUESTED)")
    private String infoRequested;

    @Schema(description = "Whether the reviewer agrees with the AI escalation recommendation")
    private Boolean agreedWithEscalation;

    @Schema(description = "Optional statement-level edits with reasons")
    private List<StatementEdit> edits;
}