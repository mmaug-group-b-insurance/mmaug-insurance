package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope;
import com.mmaug.claimsagent.common.exception.*;
import com.mmaug.claimsagent.tools.dto.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.function.*;

/** Read-only, in-process synthetic policy data. No model-controlled arguments. */
@Service
public class CaseSupportingLookups {
 private final CoverageLookupTool coverage;
 private final ClaimHistoryTool history;
 private final ToolCallRecorder recorder;
 private final BusinessRulesTool rules;
 public CaseSupportingLookups(CoverageLookupTool coverage, ClaimHistoryTool history, ToolCallRecorder recorder, BusinessRulesTool rules) {
  this.coverage=coverage; this.history=history; this.recorder=recorder; this.rules=rules;
 }
 public ToolEnvelope<CoverageView> coverage(AgentRun run) {
  return lookup(run,"get_coverage",()->coverage.lookup(run.getClaim().getPolicyNumberStated()),CoverageView::getPolicyNumber,false);
 }
 public ToolEnvelope<HistoryView> history(AgentRun run) {
  return lookup(run,"get_claim_history",()->history.lookup(run.getClaim().getPolicyNumberStated(),5),HistoryView::getPolicyNumber,true);
 }
 public ToolEnvelope<RulesView> rules(AgentRun run, ToolEnvelope<PolicyView> policy) {
  // Policy product comes only from this claim's lookup, never model input.
  return lookup(run,"get_business_rules",()->{
   if(policy==null) throw new IllegalStateException("Missing policy outcome");
   if("NOT_FOUND".equals(policy.getStatus())) throw new NotFoundException("No policy");
   if("DENIED".equals(policy.getStatus())) throw new ForbiddenException("Policy denied");
   if(!("tool:get_policy:claim:"+run.getClaim().getId()+":run:"+run.getId()).equals(policy.getSourceRef())) throw new IllegalStateException("Unbound policy snapshot");
   if(!"OK".equals(policy.getStatus()) || policy.getData()==null || !run.getClaim().getPolicyNumberStated().equals(policy.getData().getPolicyNumber())) throw new IllegalStateException("Unusable policy");
   return rules.load(policy.getData().getProductCode());
  },data->run.getClaim().getPolicyNumberStated(),false);
 }
 private <T> ToolEnvelope<T> lookup(AgentRun run,String tool,Supplier<T> action,Function<T,String> identity,boolean historyCall) {
  if(run==null || run.getId()==null || run.getClaim()==null || run.getClaim().getId()==null) throw new IllegalArgumentException("Persisted claim-bound run required");
  String number=run.getClaim().getPolicyNumberStated();
  String source="tool:"+tool+":claim:"+run.getClaim().getId()+":run:"+run.getId();
  long start=System.nanoTime(); ToolEnvelope<T> result; ToolCallStatus status;
  try {
   if(number==null || number.isBlank()) throw new NotFoundException("Missing policy");
   T data=action.get();
   if(data==null || !number.equals(identity.apply(data))) throw new IllegalStateException("Policy identity mismatch");
   result=ToolEnvelope.ok(data,source,"v1"); status=ToolCallStatus.SUCCESS;
  } catch(NotFoundException e) {
   result=ToolEnvelope.notFound("get_business_rules".equals(tool)?"RULES_NOT_FOUND":"POLICY_NOT_FOUND","get_business_rules".equals(tool)?"No applicable synthetic rules were found":"No matching policy was found",source,"v1"); status=ToolCallStatus.NOT_FOUND;
  } catch(org.springframework.security.access.AccessDeniedException | ForbiddenException e) {
   result=ToolEnvelope.error("TOOL_ACCESS_DENIED","Lookup access denied",source,"v1");result.setStatus("DENIED");status=ToolCallStatus.DENIED;
  } catch(Exception e) {
   result=ToolEnvelope.error("TOOL_UNAVAILABLE","Lookup unavailable",source,"v1");result.setStatus("UNAVAILABLE");status=ToolCallStatus.FAILED;
  }
  var args=new java.util.LinkedHashMap<String,Object>();args.put("claimId",run.getClaim().getId());args.put("policyNumber",number);
  if(historyCall) args.put("yearsBack",5);
  var json=JsonMapper.builder().build();
  recorder.record(run,null,tool,"v1",json.writeValueAsString(args),json.writeValueAsString(result),status,null,(int)((System.nanoTime()-start)/1_000_000),result.getErrorCode(),result.getErrorMessage());
  return result;
 }
}
