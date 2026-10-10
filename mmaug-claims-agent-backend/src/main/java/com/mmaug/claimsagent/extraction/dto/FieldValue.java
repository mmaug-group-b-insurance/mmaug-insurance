package com.mmaug.claimsagent.extraction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A field value extracted from a document, with provenance")
public class FieldValue {

    @Schema(description = "Extracted value as a string")
    private String value;

    @Schema(description = "Confidence between 0.000 and 1.000")
    private BigDecimal confidence;

    @Schema(description = "Source document ID")
    private UUID sourceDocumentId;

    @Schema(description = "Page number within the source document")
    private Integer page;

    @Schema(description = "Text snippet around the value")
    private String snippet;
}