package com.mmaug.claimsagent.agent.summary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name="app.ai.summary-provider",havingValue="deterministic",matchIfMissing=true)
public class DeterministicClaimAnalysis implements ClaimAnalysisService {
    @Override public Result organize(GroundedSummaryBuilder.Draft draft) {
        return new Result(draft,"grounded-template-v2",null,null,null);
    }
}
