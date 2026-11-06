package com.mmaug.claimsagent.agent.tools;
import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.tools.dto.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CaseSupportingLookupsTest {
 private final CoverageLookupTool coverage=mock(CoverageLookupTool.class);
 private final ClaimHistoryTool history=mock(ClaimHistoryTool.class);
 private final ToolCallRecorder recorder=mock(ToolCallRecorder.class);
 private final CaseSupportingLookups tools=new CaseSupportingLookups(coverage,history,recorder,new BusinessRulesTool());
 private AgentRun run(){var c=new Claim();c.setId(UUID.randomUUID());c.setPolicyNumberStated("SYN-1");var r=new AgentRun();r.setId(UUID.randomUUID());r.setClaim(c);return r;}
 @Test void successfulEmptyDataRemainsSuccessful(){when(coverage.lookup("SYN-1")).thenReturn(CoverageView.builder().policyNumber("SYN-1").coverages(List.of()).build());when(history.lookup("SYN-1",5)).thenReturn(HistoryView.builder().policyNumber("SYN-1").totalClaims(0).claims(List.of()).build());assertThat(tools.coverage(run()).getStatus()).isEqualTo("OK");assertThat(tools.history(run()).getData().getTotalClaims()).isZero();verify(history).lookup("SYN-1",5);verify(recorder,times(2)).record(any(),isNull(),anyString(),eq("v1"),anyString(),anyString(),eq(ToolCallStatus.SUCCESS),isNull(),anyInt(),isNull(),isNull());}
 @Test void noMatchDenialAndUnavailableRemainDistinct(){when(coverage.lookup(any())).thenThrow(new com.mmaug.claimsagent.common.exception.NotFoundException("private"));assertThat(tools.coverage(run()).getStatus()).isEqualTo("NOT_FOUND");when(history.lookup(any(),anyInt())).thenThrow(new org.springframework.security.access.AccessDeniedException("private"));assertThat(tools.history(run()).getStatus()).isEqualTo("DENIED");doThrow(new IllegalStateException("private")).when(history).lookup(any(),anyInt());var result=tools.history(run());assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");assertThat(result.getData()).isNull();assertThat(result.getErrorMessage()).doesNotContain("private");}
 @Test void unboundRunIsRejectedBeforeToolCall(){assertThatThrownBy(()->tools.history(new AgentRun())).isInstanceOf(IllegalArgumentException.class);verifyNoInteractions(history,coverage,recorder);}
 @Test void wrongPolicyIdentityIsNotSuccess(){when(coverage.lookup(any())).thenReturn(CoverageView.builder().policyNumber("OTHER").build());assertThat(tools.coverage(run()).getStatus()).isEqualTo("UNAVAILABLE");}
 @Test void rulesAreBoundToPolicyProductAndRun(){
  var r=run();var p=PolicyView.builder().policyNumber("SYN-1").productCode("PRIVATE_MOTOR").build();
  var envelope=com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.ok(p,"tool:get_policy:claim:"+r.getClaim().getId()+":run:"+r.getId(),"v1");
  assertThat(tools.rules(r,envelope).getData().getRuleSetVersion()).isEqualTo("synthetic-1.0.0");
  p.setProductCode("UNSUPPORTED");assertThat(tools.rules(r,envelope).getStatus()).isEqualTo("NOT_FOUND");
  p.setProductCode("PRIVATE_MOTOR");envelope.setSourceRef("another-run");assertThat(tools.rules(r,envelope).getStatus()).isEqualTo("UNAVAILABLE");
 }
 @Test void rulesDoNotInventDataWhenPolicyFails(){
  var r=run();var p=com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope.<PolicyView>notFound("POLICY_NOT_FOUND","No match","test","v1");
  assertThat(tools.rules(r,p).getStatus()).isEqualTo("NOT_FOUND");
  p.setStatus("DENIED");assertThat(tools.rules(r,p).getStatus()).isEqualTo("DENIED");
  p.setStatus("UNAVAILABLE");assertThat(tools.rules(r,p).getStatus()).isEqualTo("UNAVAILABLE");
 }
}
