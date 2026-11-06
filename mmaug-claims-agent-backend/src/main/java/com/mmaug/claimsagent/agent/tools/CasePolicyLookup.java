package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

/** Internal fixed-workflow adapter. Arguments are bound to the run's claim, never supplied by a model. */
@Service
public class CasePolicyLookup {
    private final PolicyLookupTool policy;
    private final ToolCallRecorder recorder;
    public CasePolicyLookup(PolicyLookupTool policy, ToolCallRecorder recorder) {
        this.policy = policy;
        this.recorder = recorder;
    }
    public ToolEnvelope<PolicyView> lookup(AgentRun run) {
        if (run == null || run.getId() == null || run.getClaim() == null || run.getClaim().getId() == null) {
            throw new IllegalArgumentException("A persisted claim-bound run is required");
        }
        String number = run.getClaim().getPolicyNumberStated();
        String source = "tool:get_policy:claim:" + run.getClaim().getId() + ":run:" + run.getId();
        long started = System.nanoTime();
        ToolEnvelope<PolicyView> result;
        ToolCallStatus status;
        try {
            if (number == null || number.isBlank()) throw new NotFoundException("No stated policy number");
            var view = policy.lookup(number);
            if (view == null || !number.equals(view.getPolicyNumber())) {
                throw new IllegalStateException("Policy identity mismatch");
            }
            result = ToolEnvelope.ok(view, source, "v1");
            status = ToolCallStatus.SUCCESS;
        } catch (NotFoundException e) {
            result = ToolEnvelope.notFound("POLICY_NOT_FOUND", "No matching policy was found", source, "v1");
            status = ToolCallStatus.NOT_FOUND;
        } catch (org.springframework.security.access.AccessDeniedException | com.mmaug.claimsagent.common.exception.ForbiddenException e) {
            result = ToolEnvelope.error("POLICY_ACCESS_DENIED", "Policy lookup access denied", source, "v1");
            result.setStatus("DENIED");
            status = ToolCallStatus.DENIED;
        } catch (Exception e) {
            result = ToolEnvelope.error("POLICY_UNAVAILABLE", "Policy lookup unavailable", source, "v1");
            result.setStatus("UNAVAILABLE");
            status = ToolCallStatus.FAILED;
        }
        var mapper = JsonMapper.builder().build();
        recorder.record(run, null, "get_policy", "v1",
                mapper.writeValueAsString(Map.of("claimId", run.getClaim().getId().toString(),
                        "policyNumber", number == null ? "" : number)),
                mapper.writeValueAsString(result), status, null,
                (int) ((System.nanoTime() - started) / 1_000_000), result.getErrorCode(), result.getErrorMessage());
        return result;
    }
}
