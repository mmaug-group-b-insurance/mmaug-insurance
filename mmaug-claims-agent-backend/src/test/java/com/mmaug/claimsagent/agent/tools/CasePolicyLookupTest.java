package com.mmaug.claimsagent.agent.tools;
import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CasePolicyLookupTest {
 private final PolicyLookupTool policy=mock(PolicyLookupTool.class);
 private final ToolCallRecorder recorder=mock(ToolCallRecorder.class);
 private final CasePolicyLookup gateway=new CasePolicyLookup(policy,recorder);
 private AgentRun run(){Claim c=new Claim();c.setId(UUID.randomUUID());c.setPolicyNumberStated("POL-MT-SYN001");AgentRun r=new AgentRun();r.setId(UUID.randomUUID());r.setClaim(c);return r;}
 @Test void bindsLookupToClaimAndAuditsSnapshot(){var run=run();when(policy.lookup("POL-MT-SYN001")).thenReturn(PolicyView.builder().policyNumber("POL-MT-SYN001").status("ACTIVE").build());var result=gateway.lookup(run);assertThat(result.getStatus()).isEqualTo("OK");assertThat(result.getSourceRef()).contains(run.getId().toString(),run.getClaim().getId().toString());verify(recorder).record(eq(run),isNull(),eq("get_policy"),eq("v1"),contains("POL-MT-SYN001"),contains("ACTIVE"),eq(ToolCallStatus.SUCCESS),isNull(),anyInt(),isNull(),isNull());}
 @Test void noMatchIsNotUnavailable(){when(policy.lookup(any())).thenThrow(new NotFoundException("private"));assertThat(gateway.lookup(run()).getStatus()).isEqualTo("NOT_FOUND");}
 @Test void denialIsDistinctAndSanitized(){when(policy.lookup(any())).thenThrow(new org.springframework.security.access.AccessDeniedException("private"));var result=gateway.lookup(run());assertThat(result.getStatus()).isEqualTo("DENIED");assertThat(result.getErrorMessage()).doesNotContain("private");}
 @Test void unavailableDoesNotFabricateData(){when(policy.lookup(any())).thenThrow(new IllegalStateException("private endpoint"));var result=gateway.lookup(run());assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");assertThat(result.getData()).isNull();assertThat(result.getErrorMessage()).doesNotContain("private endpoint");}
 @Test void rejectsUnboundRunBeforeLookup(){assertThatThrownBy(()->gateway.lookup(new AgentRun())).isInstanceOf(IllegalArgumentException.class);verifyNoInteractions(policy,recorder);}
 @Test void wrongPolicyIdentityIsUnavailable(){when(policy.lookup(any())).thenReturn(PolicyView.builder().policyNumber("OTHER").build());assertThat(gateway.lookup(run()).getStatus()).isEqualTo("UNAVAILABLE");}
}
