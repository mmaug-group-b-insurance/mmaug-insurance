package com.mmaug.claimsagent.agent;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.agent.tools.ToolCall;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class AgentRunResponsesTest {
 @Test void exposesFlatContractWithoutEntityGraphAndParsesJsonFields(){
  Claim claim=new Claim();claim.setId(UUID.randomUUID());claim.setClaimantName("PRIVATE CLAIM DATA");
  AgentRun run=new AgentRun();run.setId(UUID.randomUUID());run.setClaim(claim);run.setWarnings("[\"Coverage pending\"]");
  AgentStep step=new AgentStep();step.setStepType(AgentStepType.TOOL_CALL);step.setOutputSummary("{\"summary\":\"get_policy outcome=OK\"}");
  ToolCall call=new ToolCall();call.setArgumentsJson("{\"policyNumber\":\"SYN\"}");call.setResultJson("{\"status\":\"OK\"}");
  var json=JsonMapper.builder().build().valueToTree(AgentRunResponses.detail(run,List.of(step),List.of(call)));
  assertThat(json.path("runId").asText()).isEqualTo(run.getId().toString());
  assertThat(json.path("claimId").asText()).isEqualTo(claim.getId().toString());
  assertThat(json.path("warnings").isArray()).isTrue();
  assertThat(json.path("steps").get(0).path("type").asText()).isEqualTo("TOOL_CALL");
  assertThat(json.path("toolCalls").get(0).path("resultEnvelope").path("status").asText()).isEqualTo("OK");
  assertThat(json.has("run")).isFalse();assertThat(json.toString()).doesNotContain("PRIVATE CLAIM DATA");
 }
}
