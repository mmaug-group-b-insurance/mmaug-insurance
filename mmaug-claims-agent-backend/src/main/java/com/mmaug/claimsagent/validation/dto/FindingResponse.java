package com.mmaug.claimsagent.validation.dto;

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
@Schema(description = "Validation finding")
public class FindingResponse {

    private UUID id;
    private UUID claimId;
    private String type;
    private String severity;
    private String origin;
    private String fieldName;
    private String message;
    private List<String> evidence;
    private Boolean resolved;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String resolutionNote;
    private Instant createdAt;
}