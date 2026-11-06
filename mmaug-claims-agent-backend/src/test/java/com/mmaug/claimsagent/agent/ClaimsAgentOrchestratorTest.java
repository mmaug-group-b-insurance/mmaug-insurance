package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.agent.guardrails.*;
import com.mmaug.claimsagent.agent.summary.*;
import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.document.*;
import com.mmaug.claimsagent.extraction.*;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import com.mmaug.claimsagent.validation.FindingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClaimsAgentOrchestratorTest {
    private final AgentRunRepository runs = mock(AgentRunRepository.class);
    private final AgentStepRepository steps = mock(AgentStepRepository.class);
    private final ClaimRepository claims = mock(ClaimRepository.class);
    private final ClaimDocumentRepository documents = mock(ClaimDocumentRepository.class);
    private final ClaimSummaryRepository summaries = mock(ClaimSummaryRepository.class);
    private final SummaryStatementRepository statements = mock(SummaryStatementRepository.class);
    private final ExtractionService extraction = mock(ExtractionService.class);
    private final FactConsolidator consolidator = mock(FactConsolidator.class);
    private final GroundedSummaryBuilder generator = mock(GroundedSummaryBuilder.class);
    private final FindingService validation = mock(FindingService.class);
    private final List<AgentStep> savedSteps = new ArrayList<>();
    private final Claim claim = new Claim();
    private final AgentRun reserved = new AgentRun();
    private final com.mmaug.claimsagent.agent.tools.CasePolicyLookup policyLookup = mock(com.mmaug.claimsagent.agent.tools.CasePolicyLookup.class);
    private final com.mmaug.claimsagent.agent.tools.CaseSupportingLookups supporting = mock(com.mmaug.claimsagent.agent.tools.CaseSupportingLookups.class);
    private final AnalysisInputGuard inputs=mock(AnalysisInputGuard.class);
    private final EvidenceVerifier verifier=mock(EvidenceVerifier.class);
    private final ClaimsAgentOrchestrator orchestrator = new ClaimsAgentOrchestrator(runs, steps, claims,
            documents, summaries, statements, extraction, consolidator, mock(PromptInjectionDetector.class),
            mock(InputSanitizer.class), new EscalationEvaluator(), generator, validation, policyLookup, supporting,
            mock(com.mmaug.claimsagent.validation.PolicyEvidenceValidator.class), inputs, verifier);

    @BeforeEach void setup() {
        when(verifier.verify(any(),any())).thenReturn(true);
        when(policyLookup.lookup(any())).thenReturn(com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.notFound("POLICY_NOT_FOUND", "No match", "test", "v1"));
        when(supporting.coverage(any())).thenReturn(com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.notFound("POLICY_NOT_FOUND", "No match", "test", "v1"));
        when(supporting.rules(any(),any())).thenReturn(com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.notFound("RULES_NOT_FOUND", "No match", "test", "v1"));
        when(supporting.history(any())).thenReturn(com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.notFound("POLICY_NOT_FOUND", "No match", "test", "v1"));
        claim.setId(UUID.randomUUID()); claim.setClaimNumber("SYNTHETIC-001");
        reserved.setId(UUID.randomUUID());reserved.setClaim(claim);reserved.setStatus(AgentRunStatus.RUNNING);reserved.setExpiresAt(java.time.Instant.now().plusSeconds(1800));
        when(runs.findById(reserved.getId())).thenReturn(Optional.of(reserved));
        when(claims.findForDocumentUpload(claim.getId())).thenReturn(Optional.of(claim));
        when(runs.save(any())).thenAnswer(i -> {
            AgentRun run = i.getArgument(0); if (run.getId() == null) run.setId(UUID.randomUUID()); return run;
        });
        when(steps.save(any())).thenAnswer(i -> { savedSteps.add(i.getArgument(0)); return i.getArgument(0); });
        when(summaries.save(any())).thenAnswer(i -> i.getArgument(0));
        when(summaries.findAllByClaimIdOrderByVersionNoDesc(any())).thenReturn(List.of());
        when(validation.runValidation(any(), any())).thenReturn(List.of());
        when(generator.build(any(), any())).thenReturn(new GroundedSummaryBuilder.Draft(
                "Synthetic template", "LOW", List.of(new GroundedStatement("INCIDENT",StatementType.EXTRACTED_FACT,"Synthetic quotation",null,List.of())), List.of("Unknown policy")));
    }

    private AgentRun execute(){var p=orchestrator.prepareReserved(reserved.getId());return p==null?reserved:orchestrator.publishPrepared(p,new DeterministicClaimAnalysis().organize(p.draft()));}

    private void clearedExtraction() {
        ClaimDocument document = new ClaimDocument(); document.setId(UUID.randomUUID()); document.setClaim(claim);
        document.setScanStatus(ScanStatus.CLEAN); document.setProcessingStatus(ProcessingStatus.EXTRACTED);
        when(documents.findAllByClaimIdAndDeletedAtIsNull(claim.getId())).thenReturn(List.of(document));
        when(extraction.getForDocument(document.getId())).thenReturn(ExtractionResponse.builder()
                .extractionId(UUID.randomUUID()).status(ExtractionStatus.SUCCEEDED).injectionSuspected(false).build());
    }

    @Test void incompleteCapabilitiesCannotProduceAnUnqualifiedSuccess() {
        clearedExtraction();
        AgentRun result = execute();
        assertThat(result.getStatus()).isEqualTo(AgentRunStatus.COMPLETED_WITH_WARNINGS);
        assertThat(result.getWarnings()).contains("full business-rule evaluation", "coverage interpretation", "pending business validation");
        assertThat(savedSteps).anyMatch(s -> s.getStepType() == AgentStepType.TOOL_CALL && s.getStatus().equals("SUCCESS")
                && s.getOutputSummary().contains("NOT_FOUND"));
        assertThat(savedSteps).anyMatch(s -> s.getStepType() == AgentStepType.VERIFY_EVIDENCE && s.getStatus().equals("SUCCESS"));
        verify(validation).runValidation(claim.getId(), result.getId());
        verify(consolidator).consolidate(eq(claim), any());
        var captured = org.mockito.ArgumentCaptor.forClass(ClaimSummary.class);
        verify(summaries, atLeastOnce()).save(captured.capture());
        assertThat(captured.getValue().getDisclaimer()).contains("Source-linked deterministic draft");
        assertThat(captured.getValue().getLlmModel()).isEqualTo("grounded-template-v2");
    }

    @Test void unavailableLookupIsAFailedStepEvenWhenPartialSummaryCanBePrepared() {
        clearedExtraction();
        var unavailable=com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.<com.mmaug.claimsagent.tools.dto.CoverageView>error("TOOL_UNAVAILABLE", "Unavailable", "test", "v1");unavailable.setStatus("UNAVAILABLE");
        when(supporting.coverage(any())).thenReturn(unavailable);
        AgentRun result=execute();
        assertThat(result.getStatus()).isEqualTo(AgentRunStatus.ESCALATED);
        assertThat(savedSteps).anyMatch(step->step.getName().equals("Coverage lookup")&&step.getStatus().equals("FAILED")&&step.getErrorCode().equals("TOOL_UNAVAILABLE"));
    }

    @Test void failureAfterSummaryPersistenceEscapesSoWholeProcessingTransactionRollsBack() {
        clearedExtraction();
        when(statements.save(any())).thenThrow(new IllegalStateException("Synthetic persistence failure"));
        when(generator.build(any(),any())).thenReturn(new GroundedSummaryBuilder.Draft("Synthetic", "LOW", List.of(new GroundedStatement("INCIDENT",StatementType.EXTRACTED_FACT,"Synthetic source quotation",null,List.of())),List.of()));
        assertThatThrownBy(()->execute()).isInstanceOf(IllegalStateException.class).hasMessage("Processing output could not be finalized");
    }

    @Test void missingEvidenceFailsWithoutCreatingSummary() {
        when(documents.findAllByClaimIdAndDeletedAtIsNull(claim.getId())).thenReturn(List.of());
        assertThat(execute().getStatus()).isEqualTo(AgentRunStatus.FAILED);
        verifyNoInteractions(generator, validation, summaries);
    }

    @Test void validationFailureRecordsFailedStepAndStopsSummaryGeneration() {
        clearedExtraction();
        when(validation.runValidation(any(), any())).thenThrow(new IllegalStateException("private provider diagnostics"));
        AgentRun result = execute();
        assertThat(result.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(result.getErrorMessage()).doesNotContain("private provider diagnostics");
        assertThat(savedSteps).anyMatch(s -> s.getStepType() == AgentStepType.VALIDATE && s.getStatus().equals("FAILED"));
        verifyNoInteractions(generator, summaries);
    }
}
