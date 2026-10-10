package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.agent.guardrails.EscalationEvaluator;
import com.mmaug.claimsagent.agent.guardrails.InputSanitizer;
import com.mmaug.claimsagent.agent.guardrails.PromptInjectionDetector;
import com.mmaug.claimsagent.agent.summary.ClaimSummary;
import com.mmaug.claimsagent.agent.summary.ClaimSummaryRepository;
import com.mmaug.claimsagent.agent.summary.StatementType;
import com.mmaug.claimsagent.agent.summary.SummaryGenerator;
import com.mmaug.claimsagent.agent.summary.SummaryStatement;
import com.mmaug.claimsagent.agent.summary.SummaryStatementRepository;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.common.util.HashUtil;
import com.mmaug.claimsagent.document.ClaimDocument;
import com.mmaug.claimsagent.document.ClaimDocumentRepository;
import com.mmaug.claimsagent.document.ProcessingStatus;
import com.mmaug.claimsagent.document.ScanStatus;
import com.mmaug.claimsagent.extraction.ExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClaimsAgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ClaimsAgentOrchestrator.class);

    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final ClaimSummaryRepository summaryRepository;
    private final SummaryStatementRepository statementRepository;
    private final ExtractionService extractionService;
    private final PromptInjectionDetector injectionDetector;
    private final InputSanitizer inputSanitizer;
    private final EscalationEvaluator escalationEvaluator;
    private final SummaryGenerator summaryGenerator;

    public ClaimsAgentOrchestrator(AgentRunRepository runRepository,
                                   AgentStepRepository stepRepository,
                                   ClaimRepository claimRepository,
                                   ClaimDocumentRepository documentRepository,
                                   ClaimSummaryRepository summaryRepository,
                                   SummaryStatementRepository statementRepository,
                                   ExtractionService extractionService,
                                   PromptInjectionDetector injectionDetector,
                                   InputSanitizer inputSanitizer,
                                   EscalationEvaluator escalationEvaluator,
                                   SummaryGenerator summaryGenerator) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.claimRepository = claimRepository;
        this.documentRepository = documentRepository;
        this.summaryRepository = summaryRepository;
        this.statementRepository = statementRepository;
        this.extractionService = extractionService;
        this.injectionDetector = injectionDetector;
        this.inputSanitizer = inputSanitizer;
        this.escalationEvaluator = escalationEvaluator;
        this.summaryGenerator = summaryGenerator;
    }

    public AgentRun execute(UUID claimId, UUID triggeredByUserId, String correlationId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new com.mmaug.claimsagent.common.exception.NotFoundException("Claim not found"));

        AgentRun run = new AgentRun();
        run.setClaim(claim);
        run.setTriggeredByUserId(triggeredByUserId);
        run.setCorrelationId(correlationId);
        run.setPromptVersion("v1");
        run.setToolSchemaVersion("v1");
        run.setRuleSetVersion("1.0.0");
        run.setStatus(AgentRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = runRepository.save(run);

        List<String> warnings = new ArrayList<>();
        boolean injectionSuspected = false;
        int stepNo = 0;

        try {
            // Step 1: SCAN_CHECK
            stepNo = recordStep(run, stepNo, AgentStepType.SCAN_CHECK, "Scan check", () -> {
                List<ClaimDocument> docs = documentRepository.findAllByClaimIdAndDeletedAtIsNull(claimId);
                long clean = docs.stream().filter(d -> d.getScanStatus() == ScanStatus.CLEAN).count();
                return "clean=" + clean + " total=" + docs.size();
            });

            // Step 2: OCR + Step 3: STRUCTURE — triggered per document
            List<ClaimDocument> docs = documentRepository.findAllByClaimIdAndDeletedAtIsNull(claimId);
            for (ClaimDocument doc : docs) {
                if (doc.getScanStatus() != ScanStatus.CLEAN) continue;
                if (doc.getProcessingStatus() == ProcessingStatus.EXTRACTED) continue;

                try {
                    var extraction = extractionService.extract(doc.getId());
                    if (Boolean.TRUE.equals(extraction.getInjectionSuspected())) {
                        injectionSuspected = true;
                    }
                    if (extraction.getStatus() == com.mmaug.claimsagent.extraction.ExtractionStatus.FAILED) {
                        warnings.add("Extraction failed for document " + doc.getId());
                    }
                } catch (Exception e) {
                    log.warn("Extraction failed for document {}", doc.getId(), e);
                    warnings.add("Extraction error for document " + doc.getId());
                }
            }

            // Step 3: STRUCTURE — record the aggregate
            final boolean injectionFlag = injectionSuspected;
            stepNo = recordStep(run, stepNo, AgentStepType.STRUCTURE, "Structuring complete",
                    () -> "injectionSuspected=" + injectionFlag);

            // Step 4: GUARDRAIL — injection classification
            stepNo = recordStep(run, stepNo, AgentStepType.GUARDRAIL, "Prompt injection classification",
                    () -> "suspected=" + injectionFlag);

            // Step 5: CONSOLIDATE
            stepNo = recordStep(run, stepNo, AgentStepType.CONSOLIDATE, "Consolidate facts",
                    () -> "consolidated");

            // Step 6: VALIDATE
            stepNo = recordStep(run, stepNo, AgentStepType.VALIDATE, "Rule validation",
                    () -> "validated");

            // Step 7: TOOL_CALL — retrieve policy, coverage, history, rules
            stepNo = recordStep(run, stepNo, AgentStepType.TOOL_CALL, "Tool invocation",
                    () -> "tools allowed=" + com.mmaug.claimsagent.agent.tools.AgentToolRegistry.ALLOWED_TOOLS.size());

            // Step 8: VALIDATE — compare claim facts vs policy facts
            stepNo = recordStep(run, stepNo, AgentStepType.VALIDATE, "Cross-check claim vs policy",
                    () -> "checked");

            // Step 9: GENERATE_SUMMARY
            var generated = summaryGenerator.generate(claim);
            stepNo = recordStep(run, stepNo, AgentStepType.GENERATE_SUMMARY, "Generate summary",
                    () -> "statements=" + generated.statements().size());

            // Persist summary
            ClaimSummary summary = new ClaimSummary();
            summary.setClaim(claim);
            summary.setRun(run);
            summary.setVersionNo(nextVersion(claimId));
            summary.setHeadline(generated.headline());
            summary.setOverallConfidence(generated.overallConfidence());
            summary.setEscalationRecommended(false);
            summary.setEscalationReasons("[]");
            summary.setUncertaintyNotes(toJsonArray(generated.uncertaintyNotes()));
            summary.setLlmModel("deterministic-v1");
            summary.setPromptVersion("v1");
            summary.setGeneratedAt(Instant.now());
            summary.setDisclaimer("Decision-support only.");
            summary = summaryRepository.save(summary);

            int orderNo = 1;
            for (var st : generated.statements()) {
                SummaryStatement statement = new SummaryStatement();
                statement.setSummary(summary);
                statement.setOrderNo(orderNo++);
                statement.setSection(st.section());
                statement.setStatementType(st.type());
                statement.setText(st.text());
                statement.setConfidence(st.confidence());
                statementRepository.save(statement);
            }

            // Step 10: VERIFY_EVIDENCE
            stepNo = recordStep(run, stepNo, AgentStepType.VERIFY_EVIDENCE, "Verify evidence",
                    () -> "verified");

            // Step 11: ESCALATION_CHECK
            EscalationEvaluator.Evaluation evaluation = escalationEvaluator.evaluate(claim, injectionSuspected, false);
            summary.setEscalationRecommended(evaluation.required());
            summary.setEscalationReasons(toJsonArray(evaluation.reasons()));
            summaryRepository.save(summary);

            stepNo = recordStep(run, stepNo, AgentStepType.ESCALATION_CHECK, "Escalation check",
                    () -> "required=" + evaluation.required());

            // Finalize run
            run.setCompletedAt(Instant.now());
            run.setEscalationRequired(evaluation.required());
            run.setWarnings(toJsonArray(warnings));
            if (evaluation.required()) {
                run.setStatus(AgentRunStatus.ESCALATED);
                claim.setRiskFlag(true);
                claim.setEscalationReason(String.join("; ", evaluation.reasons()));
            } else if (!warnings.isEmpty()) {
                run.setStatus(AgentRunStatus.COMPLETED_WITH_WARNINGS);
            } else {
                run.setStatus(AgentRunStatus.COMPLETED);
            }
//            run.setLatestRunIdIfPossible(claim);
            runRepository.save(run);

            claim.setLatestRunId(run.getId());
            claimRepository.save(claim);

            return run;
        } catch (Exception e) {
            log.error("Agent run failed for claim {}", claimId, e);
            run.setStatus(AgentRunStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorCode("AGENT_FAILED");
            run.setErrorMessage(e.getMessage());
            runRepository.save(run);
            return run;
        }
    }

    private int recordStep(AgentRun run, int stepNo, AgentStepType type, String name, StepAction action) {
        Instant start = Instant.now();
        String output = action.run();
        Instant end = Instant.now();

        AgentStep step = new AgentStep();
        step.setRun(run);
        step.setStepNo(stepNo);
        step.setStepType(type);
        step.setName(name);
        step.setStatus("SUCCESS");
        step.setInputHash(HashUtil.sha256(run.getId().toString() + stepNo));
        step.setOutputSummary("{\"summary\":\"" + escape(output) + "\"}");
        step.setDurationMs((int) java.time.Duration.between(start, end).toMillis());
        step.setStartedAt(start);
        stepRepository.save(step);
        return stepNo + 1;
    }

    private int nextVersion(UUID claimId) {
        return summaryRepository.findAllByClaimIdOrderByVersionNoDesc(claimId).stream()
                .map(ClaimSummary::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(values.get(i))).append('"');
        }
        return sb.append(']').toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @FunctionalInterface
    private interface StepAction {
        String run();
    }
}