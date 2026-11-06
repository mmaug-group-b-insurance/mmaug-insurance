package com.mmaug.claimsagent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** One execution per reservation; no automatic retries of paid provider operations. */
@Service
public class AgentExecutionService {
    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);
    private final AgentRunLifecycle lifecycle;
    private final ClaimsAgentOrchestrator processor;
    private final AnalysisInputGuard inputs;
    private final com.mmaug.claimsagent.extraction.ExtractionService extraction;
    private final com.mmaug.claimsagent.agent.summary.ClaimAnalysisService analysis;

    public AgentExecutionService(AgentRunLifecycle lifecycle, ClaimsAgentOrchestrator processor,
            AnalysisInputGuard inputs, com.mmaug.claimsagent.extraction.ExtractionService extraction,
            com.mmaug.claimsagent.agent.summary.ClaimAnalysisService analysis) {
        this.lifecycle = lifecycle;
        this.processor = processor;this.inputs = inputs;this.extraction = extraction;this.analysis = analysis;
    }

    @Transactional(propagation = Propagation.NEVER)
    public AgentRun execute(UUID claimId, UUID actor, String correlation) {
        return execute(claimId, actor, correlation, null);
    }

    @Transactional(propagation = Propagation.NEVER)
    public AgentRun execute(UUID claimId, UUID actor, String correlation, String key) {
        var reservation = lifecycle.reserve(claimId, actor, correlation, key);
        if (!reservation.created()) return lifecycle.get(reservation.runId());
        if (!lifecycle.start(reservation.runId())) return lifecycle.get(reservation.runId());
        try {
            for(var doc:inputs.capture(reservation.runId())) if(doc.needsExtraction()) extraction.extract(doc.id());
            var prepared = processor.prepareReserved(reservation.runId());
            if(prepared==null)return lifecycle.get(reservation.runId());
            var result = analysis.organize(prepared.draft());
            return processor.publishPrepared(prepared,result);
        } catch (RuntimeException e) {
            log.warn("Processing transaction failed for run {} ({})", reservation.runId(), e.getClass().getSimpleName());
            String code=e instanceof AnalysisInputGuard.InputsChanged || e instanceof com.mmaug.claimsagent.common.exception.UnsafeFileException ? "INPUTS_CHANGED"
                    : e instanceof com.mmaug.claimsagent.agent.summary.AzureClaimAnalysis.AnalysisFailure ? e.getMessage()
                    : "PROCESSING_ROLLED_BACK";
            lifecycle.fail(reservation.runId(),code);
            return lifecycle.get(reservation.runId());
        }
    }
}
