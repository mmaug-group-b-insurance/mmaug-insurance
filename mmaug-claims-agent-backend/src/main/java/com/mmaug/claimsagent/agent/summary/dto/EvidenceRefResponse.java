package com.mmaug.claimsagent.agent.summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Evidence reference backing a summary statement")
public class EvidenceRefResponse {

    private String sourceType;
    private UUID documentId;
    private String documentName;
    private Integer page;
    private String snippet;
    private UUID toolCallId;
    private String toolName;
    private String ruleId;
    private String fieldName;
}