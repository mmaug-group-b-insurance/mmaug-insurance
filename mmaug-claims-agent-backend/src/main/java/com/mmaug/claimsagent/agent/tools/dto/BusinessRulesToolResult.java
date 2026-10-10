package com.mmaug.claimsagent.agent.tools.dto;

import com.mmaug.claimsagent.tools.dto.RulesView;
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
public class BusinessRulesToolResult {
    private String status;
    private RulesView data;
    private String errorCode;
    private String errorMessage;
    private String sourceRef;
}