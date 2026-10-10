package com.mmaug.claimsagent.agent.tools.dto;

import com.mmaug.claimsagent.tools.dto.PolicyView;
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
public class PolicyToolResult {
    private String status;
    private PolicyView data;
    private String errorCode;
    private String errorMessage;
    private String sourceRef;
}