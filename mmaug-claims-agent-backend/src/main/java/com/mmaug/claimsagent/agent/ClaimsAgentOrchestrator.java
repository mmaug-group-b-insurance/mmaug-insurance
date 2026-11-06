package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.agent.guardrails.EscalationEvaluator;
import com.mmaug.claimsagent.agent.guardrails.InputSanitizer;
import com.mmaug.claimsagent.agent.guardrails.PromptInjectionDetector;
import com.mmaug.claimsagent.agent.summary.ClaimSummary;
import com.mmaug.claimsagent.agent.summary.ClaimSummaryRepository;
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
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import com.mmaug.claimsagent.validation.FindingService;
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
    private final com.mmaug.claimsagent.extraction.FactConsolidator factConsolidator;
    private final PromptInjectionDetector injectionDetector;
    private final InputSanitizer inputSanitizer;
    private final EscalationEvaluator escalationEvaluator;
    private final com.mmaug.claimsagent.agent.summary.GroundedSummaryBuilder summaryBuilder;
    private final FindingService findingService;
    private final com.mmaug.claimsagent.agent.tools.CasePolicyLookup policyLookup;
    private final com.mmaug.claimsagent.agent.tools.CaseSupportingLookups supportingLookups;
    private final com.mmaug.claimsagent.validation.PolicyEvidenceValidator policyValidator;
    private final AnalysisInputGuard inputGuard;
    private final com.mmaug.claimsagent.agent.guardrails.EvidenceVerifier evidenceVerifier;

    public ClaimsAgentOrchestrator(AgentRunRepository runRepository,
                                   AgentStepRepository stepRepository,
                                   ClaimRepository claimRepository,
                                   ClaimDocumentRepository documentRepository,
                                   ClaimSummaryRepository summaryRepository,
                                   SummaryStatementRepository statementRepository,
                                   ExtractionService extractionService,
                                   com.mmaug.claimsagent.extraction.FactConsolidator factConsolidator,
                                   PromptInjectionDetector injectionDetector,
                                   InputSanitizer inputSanitizer,
                                   EscalationEvaluator escalationEvaluator,
                                   com.mmaug.claimsagent.agent.summary.GroundedSummaryBuilder summaryBuilder,
                                   FindingService findingService,
                                   com.mmaug.claimsagent.agent.tools.CasePolicyLookup policyLookup,
                                   com.mmaug.claimsagent.agent.tools.CaseSupportingLookups supportingLookups,
                                   com.mmaug.claimsagent.validation.PolicyEvidenceValidator policyValidator,
                                   AnalysisInputGuard inputGuard,
                                   com.mmaug.claimsagent.agent.guardrails.EvidenceVerifier evidenceVerifier) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.claimRepository = claimRepository;
        this.documentRepository = documentRepository;
        this.summaryRepository = summaryRepository;
        this.statementRepository = statementRepository;
        this.extractionService = extractionService;
        this.factConsolidator = factConsolidator;
        this.injectionDetector = injectionDetector;
        this.inputSanitizer = inputSanitizer;
        this.escalationEvaluator = escalationEvaluator;
        this.summaryBuilder = summaryBuilder;
        this.findingService = findingService;
        this.policyLookup = policyLookup;
        this.supportingLookups = supportingLookups;
        this.policyValidator = policyValidator;
        this.inputGuard = inputGuard;
        this.evidenceVerifier = evidenceVerifier;
    }

    public record Prepared(UUID runId, Long runVersion, int stepNo, List<String> warnings, boolean injectionSuspected,
            int policyFindings, String lookupStatus, String coverageStatus, String historyStatus, String rulesStatus,
            com.mmaug.claimsagent.tools.dto.RulesView.Thresholds thresholds,
            com.mmaug.claimsagent.agent.summary.GroundedSummaryBuilder.Draft draft) {}

    public Prepared prepareReserved(UUID runId) {
        AgentRun run = runRepository.findById(runId)
                .orElseThrow(() -> new com.mmaug.claimsagent.common.exception.NotFoundException("Run not found"));
        if (run.getStatus() != AgentRunStatus.RUNNING || run.getExpiresAt() == null || !run.getExpiresAt().isAfter(Instant.now()))
            throw new IllegalStateException("An unexpired running reservation is required");
        UUID claimId = run.getClaim().getId();
        Claim claim = claimRepository.findForDocumentUpload(claimId)
                .orElseThrow(() -> new com.mmaug.claimsagent.common.exception.NotFoundException("Claim not found"));

        List<String> warnings = new ArrayList<>();
        boolean injectionSuspected = false;
        int stepNo = 0;
        inputGuard.assertCurrent(run);

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
                if (doc.getScanStatus() != ScanStatus.CLEAN) {
                    warnings.add("Document excluded because scan clearance is missing: " + doc.getId());
                    continue;
                }
                if (doc.getProcessingStatus() == ProcessingStatus.EXTRACTED) {
                    var previous = extractionService.getForDocument(doc.getId());
                    injectionSuspected |= Boolean.TRUE.equals(previous.getInjectionSuspected());
                    continue;
                }

                warnings.add("Document has no completed extraction: " + doc.getId());
            }

            // Step 3: STRUCTURE — record the aggregate
            if (docs.stream().noneMatch(d -> d.getScanStatus() == ScanStatus.CLEAN
                    && d.getProcessingStatus() == ProcessingStatus.EXTRACTED)) {
                throw new IllegalStateException("No cleared document was successfully extracted");
            }
            final boolean injectionFlag = injectionSuspected;
            stepNo = recordStep(run, stepNo, AgentStepType.STRUCTURE, "Structuring complete",
                    () -> "injectionSuspected=" + injectionFlag);

            // Step 4: GUARDRAIL — injection classification
            stepNo = recordStep(run, stepNo, AgentStepType.GUARDRAIL, "Prompt injection classification",
                    () -> "suspected=" + injectionFlag);

            // Step 5: CONSOLIDATE
            stepNo = recordStep(run, stepNo, AgentStepType.CONSOLIDATE, "Consolidate facts",
                    () -> {
                        int count = 0;
                        for (ClaimDocument doc : docs) {
                            if (doc.getScanStatus() != ScanStatus.CLEAN
                                    || doc.getProcessingStatus() != ProcessingStatus.EXTRACTED) continue;
                            var extraction = extractionService.getForDocument(doc.getId());
                            if (extraction.getStatus() == com.mmaug.claimsagent.extraction.ExtractionStatus.FAILED) continue;
                            factConsolidator.consolidate(claim, extraction.getExtractionId());
                            count++;
                        }
                        return "extractions consolidated=" + count;
                    });

            final UUID currentRunId = run.getId();
            // Step 6: VALIDATE
            stepNo = recordStep(run, stepNo, AgentStepType.VALIDATE, "Rule validation",
                    () -> "findings=" + findingService.runValidation(claimId, currentRunId).size());

            // Step 7: TOOL_CALL — retrieve policy, coverage, history, rules
            var policyResult = policyLookup.lookup(run);
            final var lookupStatus = policyResult.getStatus();
            stepNo = recordLookupStep(run, stepNo, "Policy lookup", "get_policy", lookupStatus);
            if (!"OK".equals(lookupStatus)) warnings.add("Policy lookup outcome: " + lookupStatus);
            var coverageResult = supportingLookups.coverage(run);
            var historyResult = supportingLookups.history(run);
            stepNo = recordLookupStep(run, stepNo, "Coverage lookup", "get_coverage", coverageResult.getStatus());
            stepNo = recordLookupStep(run, stepNo, "Claims history lookup", "get_claim_history", historyResult.getStatus());
            if (!"OK".equals(coverageResult.getStatus())) warnings.add("Coverage lookup outcome: " + coverageResult.getStatus());
            if (!"OK".equals(historyResult.getStatus())) warnings.add("History lookup outcome: " + historyResult.getStatus());
            var rulesResult = supportingLookups.rules(run, policyResult);
            stepNo = recordLookupStep(run, stepNo, "Business rules lookup", "get_business_rules", rulesResult.getStatus());
            if ("OK".equals(rulesResult.getStatus())) {
                run.setRuleSetVersion(rulesResult.getData().getRuleSetVersion());
                run.setRuleSetSha256(HashUtil.sha256(JsonMapper.builder().build().writeValueAsString(rulesResult.getData())));
            } else warnings.add("Business rules lookup outcome: " + rulesResult.getStatus());
            warnings.add("Synthetic rule thresholds only; full business-rule evaluation and coverage interpretation remain pending business validation");

            // Step 8: VALIDATE — compare claim facts vs policy facts
            var checkedEvidence = new ArrayList<com.mmaug.claimsagent.extraction.dto.ExtractionResponse>();
            for (var doc : docs) {
                if (doc.getScanStatus() == ScanStatus.CLEAN && doc.getProcessingStatus() == ProcessingStatus.EXTRACTED)
                    checkedEvidence.add(extractionService.getForDocument(doc.getId()));
            }
            int policyFindings = policyValidator.validate(run, policyResult, checkedEvidence);
            if ("OK".equals(lookupStatus)) {
                stepNo = recordStep(run, stepNo, AgentStepType.VALIDATE, "Cross-check evidence vs policy", () -> "discrepancy findings=" + policyFindings);
            } else {
                stepNo = recordSkippedStep(run, stepNo, AgentStepType.VALIDATE, "Cross-check evidence vs policy", "Policy unavailable; no policy conclusions inferred");
            }
            if (policyFindings > 0) warnings.add("Evidence/policy discrepancies require human review: " + policyFindings);

            var draft = summaryBuilder.build(run, checkedEvidence);
            runRepository.saveAndFlush(run);
            return new Prepared(run.getId(),run.getVersion(),stepNo,List.copyOf(warnings),injectionSuspected,policyFindings,
                    lookupStatus,coverageResult.getStatus(),historyResult.getStatus(),rulesResult.getStatus(),
                    "OK".equals(rulesResult.getStatus()) ? rulesResult.getData().getThresholds() : null,draft);
        } catch (AnalysisInputGuard.InputsChanged e) { throw e;
        } catch (Exception e) {
            log.warn("Analysis preparation failed for run {} ({})",runId,e.getClass().getSimpleName());
            run.setStatus(AgentRunStatus.FAILED);run.setCompletedAt(Instant.now());run.setErrorCode("AGENT_FAILED");
            run.setErrorMessage("Analysis preparation failed; inspect correlated diagnostics and explicitly retry");runRepository.save(run);
            return null;
        }
    }

    public AgentRun publishPrepared(Prepared prepared, com.mmaug.claimsagent.agent.summary.ClaimAnalysisService.Result analysis) {
        AgentRun run = runRepository.findById(prepared.runId()).orElseThrow();
        UUID claimId = run.getClaim().getId();
        Claim claim = claimRepository.findForDocumentUpload(claimId).orElseThrow();
        inputGuard.assertCurrent(run);
        if (!java.util.Objects.equals(run.getVersion(),prepared.runVersion())) throw new AnalysisInputGuard.InputsChanged();
        List<String> warnings = new ArrayList<>(prepared.warnings());
        int stepNo = prepared.stepNo();
        int policyFindings = prepared.policyFindings();
        String lookupStatus = prepared.lookupStatus();
        boolean injectionSuspected = prepared.injectionSuspected();
        try {
            var generated = analysis.draft();
            if (generated.statements().isEmpty() || generated.statements().stream().anyMatch(st -> !evidenceVerifier.verify(run,st)))
                throw new IllegalStateException("Source evidence changed before publication");
            run.setLlmModel(analysis.model());
            run.setLlmParameters(analysis.parametersJson());
            run.setTokensIn(analysis.tokensIn());
            run.setTokensOut(analysis.tokensOut());
            stepNo = recordStep(run, stepNo, AgentStepType.GENERATE_SUMMARY, "Generate summary",
                    () -> "statements=" + generated.statements().size());

            if (!run.getExpiresAt().isAfter(Instant.now())) throw new IllegalStateException("Reservation expired before summary persistence");
            // Persist summary
            ClaimSummary summary = new ClaimSummary();
            summary.setClaim(claim);
            summary.setRun(run);
            summary.setVersionNo(nextVersion(claimId));
            summary.setHeadline(generated.headline());
            summary.setOverallConfidence(generated.overallConfidence());
            summary.setEscalationRecommended(false);
            summary.setEscalationReasons("[]");
            List<String> uncertaintyNotes = new ArrayList<>(generated.uncertaintyNotes());
            uncertaintyNotes.add("Lookup outcomes: policy=" + lookupStatus + ", coverage=" + prepared.coverageStatus() + ", history=" + prepared.historyStatus() + ", rules=" + prepared.rulesStatus());
            uncertaintyNotes.add("Policy discrepancy flags=" + policyFindings + "; coverage interpretation and full business-rule evaluation remain pending");
            summary.setUncertaintyNotes(toJsonArray(uncertaintyNotes));
            summary.setLlmModel(analysis.model());
            summary.setPromptVersion("v1");
            summary.setGeneratedAt(Instant.now());
            summary.setDisclaimer("Source-linked deterministic draft for human review. Citations checked against stored extraction/tool snapshots; this does not establish source truth, OCR accuracy or coverage eligibility. No claim decision, liability or payment action.");
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
                statement = statementRepository.save(statement);
                summaryBuilder.persistEvidence(statement, st.citations());
            }

            // Step 10: VERIFY_EVIDENCE
            warnings.addAll(generated.uncertaintyNotes().stream().filter(note -> note.startsWith("Unsupported") || note.startsWith("Instruction-like") || note.startsWith("Conflicting")).toList());
            stepNo = recordStep(run, stepNo, AgentStepType.VERIFY_EVIDENCE, "Verify evidence",
                    () -> "Source support verified for " + generated.statements().size() + " constrained statements; source truth remains unverified");

            // Step 11: ESCALATION_CHECK
            boolean unavailable = java.util.stream.Stream.of(lookupStatus, prepared.coverageStatus(), prepared.historyStatus(), prepared.rulesStatus())
                    .anyMatch(s -> "UNAVAILABLE".equals(s) || "DENIED".equals(s));
            EscalationEvaluator.Evaluation initial = escalationEvaluator.evaluate(claim, injectionSuspected, unavailable, prepared.thresholds());
            var reasons = new ArrayList<>(initial.reasons());
            if (policyFindings > 0) reasons.add("Evidence/policy discrepancy requires human review");
            EscalationEvaluator.Evaluation evaluation = new EscalationEvaluator.Evaluation(initial.required() || policyFindings > 0, reasons);
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
            throw new IllegalStateException("Processing output could not be finalized", e);
        }
    }

    private int recordStep(AgentRun run, int stepNo, AgentStepType type, String name, StepAction action) {
        AgentStep step = newStep(run, stepNo, type, name);
        try {
            step.setOutputSummary(JsonMapper.builder().build().writeValueAsString(Map.of("summary", action.run())));
            step.setStatus("SUCCESS");
        } catch (RuntimeException e) {
            step.setStatus("FAILED");
            step.setErrorCode("STEP_FAILED");
            throw e;
        } finally {
            step.setDurationMs((int) java.time.Duration.between(step.getStartedAt(), Instant.now()).toMillis());
            stepRepository.save(step);
        }
        return stepNo + 1;
    }

    private int recordLookupStep(AgentRun run, int stepNo, String name, String tool, String outcome) {
        AgentStep step = newStep(run, stepNo, AgentStepType.TOOL_CALL, name);
        boolean responseAvailable = "OK".equals(outcome) || "NOT_FOUND".equals(outcome);
        step.setStatus(responseAvailable ? "SUCCESS" : "FAILED");
        if (!responseAvailable) step.setErrorCode("TOOL_" + outcome);
        step.setOutputSummary(JsonMapper.builder().build().writeValueAsString(Map.of("summary", tool + " outcome=" + outcome)));
        step.setDurationMs(0);
        stepRepository.save(step);
        return stepNo + 1;
    }

    private int recordSkippedStep(AgentRun run, int stepNo, AgentStepType type, String name, String reason) {
        AgentStep step = newStep(run, stepNo, type, name);
        step.setStatus("SKIPPED");
        step.setOutputSummary(JsonMapper.builder().build().writeValueAsString(Map.of("summary", reason)));
        step.setDurationMs(0);
        stepRepository.save(step);
        return stepNo + 1;
    }

    private AgentStep newStep(AgentRun run, int stepNo, AgentStepType type, String name) {
        AgentStep step = new AgentStep();
        step.setRun(run);
        step.setStepNo(stepNo);
        step.setStepType(type);
        step.setName(name);
        step.setInputHash(HashUtil.sha256(run.getId().toString() + stepNo));
        step.setStartedAt(Instant.now());
        return step;
    }

    private int nextVersion(UUID claimId) {
        return summaryRepository.findAllByClaimIdOrderByVersionNoDesc(claimId).stream()
                .map(ClaimSummary::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String toJsonArray(List<String> values) {
        return JsonMapper.builder().build().writeValueAsString(values == null ? List.of() : values);
    }

    @FunctionalInterface
    private interface StepAction {
        String run();
    }
}
