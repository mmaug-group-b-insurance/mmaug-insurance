package com.mmaug.claimsagent.agent.summary.dto;

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
@Schema(description = "A single typed statement within a summary")
public class StatementResponse {

    private UUID id;
    private Integer orderNo;
    private String section;
    private String type;
    private String text;
    private BigDecimal confidence;
    private List<EvidenceRefResponse> evidence;
}