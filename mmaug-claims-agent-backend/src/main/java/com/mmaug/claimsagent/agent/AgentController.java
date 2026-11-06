package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Agent Runs", description = "Trigger, list, and inspect agent pipeline runs")
@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private final AgentExecutionService orchestrator;
    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final com.mmaug.claimsagent.agent.tools.ToolCallRepository toolCallRepository;
    private final com.mmaug.claimsagent.claim.ClaimService claimService;

    public AgentController(AgentExecutionService orchestrator,
                           AgentRunRepository runRepository,
                           AgentStepRepository stepRepository,
                           com.mmaug.claimsagent.agent.tools.ToolCallRepository toolCallRepository,
                           com.mmaug.claimsagent.claim.ClaimService claimService) {
        this.orchestrator = orchestrator;
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.toolCallRepository = toolCallRepository;
        this.claimService = claimService;
    }

    @Operation(
            summary = "Trigger agent analysis",
            description = """
                    Runs the fixed agent pipeline for a claim. The pipeline performs scan check,
                    OCR, structuring, guardrails, consolidation, validation, tool invocation,
                    summary generation, evidence verification and escalation check.
                    This operation never approves, rejects, determines liability or authorises payment.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Run completed"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only claims officers may trigger analysis",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/claims/{claimId}/analyse")
    public ResponseEntity<java.util.Map<String, UUID>> analyse(@PathVariable UUID claimId, @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, CurrentUser user) {
        if (!user.hasRole(Role.CLAIMS_OFFICER)) {
            throw new com.mmaug.claimsagent.common.exception.ForbiddenException("Only claims officers may trigger analysis");
        }
        claimService.get(claimId, user);
        AgentRun run = orchestrator.execute(claimId, UUID.fromString(user.userId()),
                org.slf4j.MDC.get("correlationId"), idempotencyKey);
        return ResponseEntity.accepted().body(java.util.Map.of("runId", run.getId()));
    }

    @Operation(summary = "List agent runs for a claim")
    @GetMapping("/claims/{claimId}/agent-runs")
    public ResponseEntity<List<java.util.Map<String,Object>>> listRuns(@PathVariable UUID claimId, CurrentUser user) {
        claimService.get(claimId, user);
        return ResponseEntity.ok(runRepository.findAllByClaimIdOrderByCreatedAtDesc(claimId).stream().map(AgentRunResponses::run).toList());
    }

    @Operation(summary = "Get agent run detail")
    @GetMapping("/agent-runs/{runId}")
    public ResponseEntity<java.util.Map<String,Object>> getRun(@PathVariable UUID runId, CurrentUser user) {
        AgentRun run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        claimService.get(run.getClaim().getId(), user);
        List<AgentStep> steps = stepRepository.findAllByRunIdOrderByStepNoAsc(runId);
        var toolCalls = toolCallRepository.findAllByRunIdOrderByCalledAtAsc(runId);
        return ResponseEntity.ok(AgentRunResponses.detail(run, steps, toolCalls));
    }

    public record AgentRunDetail(
            AgentRun run,
            List<AgentStep> steps,
            List<com.mmaug.claimsagent.agent.tools.ToolCall> toolCalls
    ) {}
}
