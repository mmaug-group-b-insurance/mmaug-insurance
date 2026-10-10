package com.mmaug.claimsagent.extraction.dto;

import com.mmaug.claimsagent.extraction.ExtractionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Extraction result for a single document")
public class ExtractionResponse {

    private UUID documentId;
    private ExtractionStatus status;
    private String extractorModel;
    private String llmModel;
    private String promptVersion;
    private String schemaVersion;
    private BigDecimal overallConfidence;
    private Boolean injectionSuspected;
    private List<ExtractedFieldDto> fields;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Single extracted field")
    public static class ExtractedFieldDto {
        private String fieldName;
        private String value;
        private String normalisedValue;
        private BigDecimal confidence;
        private Integer page;
        private String snippet;
    }
}