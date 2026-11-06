package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.agent.tools.ToolCall;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** Explicit frontend contract: never serialize JPA entities or lazy associations. */
final class AgentRunResponses {
    private AgentRunResponses() {}
    static Map<String,Object> run(AgentRun run) {
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("runId", run.getId()); dto.put("claimId", run.getClaim().getId());
        dto.put("status", run.getStatus()); dto.put("startedAt", run.getStartedAt());
        dto.put("completedAt", run.getCompletedAt()); dto.put("escalationRequired", run.getEscalationRequired());
        dto.put("warnings", json(run.getWarnings(), List.of()));
        dto.put("promptVersion", run.getPromptVersion()); dto.put("toolSchemaVersion", run.getToolSchemaVersion());
        dto.put("inputSnapshotSha256", run.getInputSnapshotSha256());
        dto.put("retryOfRunId", run.getRetryOfRunId()); dto.put("expiresAt", run.getExpiresAt());
        dto.put("ruleSetVersion", run.getRuleSetVersion()); dto.put("ruleSetSha256", run.getRuleSetSha256()); dto.put("llmModel", run.getLlmModel());
        return dto;
    }
    static Map<String,Object> detail(AgentRun run, List<AgentStep> steps, List<ToolCall> calls) {
        var dto = run(run);
        dto.put("tokensIn", run.getTokensIn()); dto.put("tokensOut", run.getTokensOut());
        dto.put("errorCode", run.getErrorCode()); dto.put("errorMessage", run.getErrorMessage());
        dto.put("steps", steps.stream().map(step -> {
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("stepNo", step.getStepNo()); out.put("type", step.getStepType());
            out.put("name", step.getName()); out.put("status", step.getStatus());
            out.put("durationMs", step.getDurationMs()); out.put("errorCode", step.getErrorCode());
            out.put("inputSummary", json(step.getInputSummary(), null));
            out.put("outputSummary", json(step.getOutputSummary(), null)); out.put("startedAt", step.getStartedAt());
            return out;
        }).toList());
        dto.put("toolCalls", calls.stream().map(call -> {
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("id", call.getId()); out.put("toolName", call.getToolName()); out.put("status", call.getStatus());
            out.put("httpStatus", call.getHttpStatus()); out.put("latencyMs", call.getLatencyMs());
            out.put("retryCount", call.getRetryCount()); out.put("arguments", json(call.getArgumentsJson(), Map.of()));
            out.put("resultEnvelope", json(call.getResultJson(), null)); out.put("calledAt", call.getCalledAt());
            out.put("errorCode", call.getErrorCode()); return out;
        }).toList());
        return dto;
    }
    private static Object json(String value, Object fallback) {
        if (value == null || value.isBlank()) return fallback;
        return JsonMapper.builder().build().readTree(value);
    }
}
