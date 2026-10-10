package com.mmaug.claimsagent.agent.tools.dto;

import com.mmaug.claimsagent.tools.dto.HistoryView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimHistoryToolResult {
    private String status;
    private HistoryView data;
    private String errorCode;
    private String errorMessage;
    private String sourceRef;
}