package com.mmaug.claimsagent.agent.summary;

/** Model boundary receives only already-verified evidence statements, never storage or tools. */
public interface ClaimAnalysisService {
    record Result(GroundedSummaryBuilder.Draft draft, String model, String parametersJson,
                  Integer tokensIn, Integer tokensOut) {}
    Result organize(GroundedSummaryBuilder.Draft draft);
}
