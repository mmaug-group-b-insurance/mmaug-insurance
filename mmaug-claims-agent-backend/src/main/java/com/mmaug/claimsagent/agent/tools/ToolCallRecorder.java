package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.AgentStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ToolCallRecorder {

    private final ToolCallRepository toolCallRepository;

    public ToolCallRecorder(ToolCallRepository toolCallRepository) {
        this.toolCallRepository = toolCallRepository;
    }

    public ToolCall record(AgentRun run,
                           AgentStep step,
                           String toolName,
                           String toolSchemaVersion,
                           String argumentsJson,
                           String resultJson,
                           ToolCallStatus status,
                           Integer httpStatus,
                           Integer latencyMs,
                           String errorCode,
                           String errorMessage) {
        ToolCall call = new ToolCall();
        call.setRun(run);
        call.setStep(step);
        call.setToolName(toolName);
        call.setToolSchemaVersion(toolSchemaVersion);
        call.setArgumentsJson(argumentsJson);
        call.setResultJson(resultJson);
        call.setStatus(status);
        call.setHttpStatus(httpStatus);
        call.setLatencyMs(latencyMs);
        call.setRetryCount(0);
        call.setErrorCode(errorCode);
        call.setErrorMessage(errorMessage);
        call.setCalledAt(Instant.now());
        return toolCallRepository.save(call);
    }
}