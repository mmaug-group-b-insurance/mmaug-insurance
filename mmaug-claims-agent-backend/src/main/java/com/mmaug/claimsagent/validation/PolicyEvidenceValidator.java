package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Discrepancy flags only. Never determines coverage, liability or a claim outcome. */
@Service
public class PolicyEvidenceValidator {
 private final ValidationFindingRepository repository;
 public PolicyEvidenceValidator(ValidationFindingRepository repository){this.repository=repository;}
 public int validate(AgentRun run,ToolEnvelope<PolicyView> envelope,List<ExtractionResponse> evidence) {
  if(!"OK".equals(envelope.getStatus()) || envelope.getData()==null) return 0;
  var policy=envelope.getData(); List<ValidationFinding> findings=new ArrayList<>();
  for(var extraction:evidence) {
   if(extraction.getStatus()==com.mmaug.claimsagent.extraction.ExtractionStatus.FAILED || extraction.getFields()==null) continue;
   for(var field:extraction.getFields()) {
    if(field.getValue()==null || field.getValue().isBlank()) continue;
    FindingType type=null; String message=null;
    switch(field.getFieldName()) {
     case "policyNumber" -> {
      if(!field.getValue().trim().equalsIgnoreCase(policy.getPolicyNumber())) {type=FindingType.CONTRADICTION;message="Evidence policy number differs from the claim-bound policy; review both sources.";}
     }
     case "vehicleRegistration" -> {
      if(policy.getVehicles()!=null && !policy.getVehicles().isEmpty() && policy.getVehicles().stream().allMatch(v->v.getRegistration()!=null)) {
       boolean match=policy.getVehicles().stream().anyMatch(v->normalize(v.getRegistration()).equals(normalize(field.getValue())));
       if(!match){type=FindingType.VEHICLE_MISMATCH;message="Evidence registration differs from listed policy vehicles; human review required.";}
      }
     }
     case "incidentDate" -> {
      try {LocalDate date=LocalDate.parse(field.getValue());
       if(policy.getStartDate()!=null && policy.getEndDate()!=null && (date.isBefore(policy.getStartDate()) || date.isAfter(policy.getEndDate()))) {
        type=FindingType.POLICY_INACTIVE_ON_DATE;message="Evidence incident date falls outside the recorded policy period; this is a discrepancy flag, not a coverage decision.";
       }
      }catch(java.time.format.DateTimeParseException ignored){/* Unknown/invalid date remains for field validation. */}
     }
    }
    if(type!=null){
     ValidationFinding f=new ValidationFinding();f.setClaim(run.getClaim());f.setAgentRun(run);f.setFindingType(type);f.setSeverity(Severity.HIGH);f.setOrigin(FindingOrigin.RULE_ENGINE);f.setFieldName(field.getFieldName());f.setMessage(message);
     f.setEvidenceRefs(JsonMapper.builder().build().writeValueAsString(List.of(
      "document:"+extraction.getDocumentId()+":extraction:"+extraction.getExtractionId()+":page:"+field.getPage()+":value:"+field.getValue(),
      envelope.getSourceRef()+":policySnapshot")));
     findings.add(f);
    }
   }
  }
  repository.saveAll(findings);return findings.size();
 }
 private static String normalize(String value){return value.toUpperCase(Locale.ROOT).replaceAll("[\\s-]","");}
}
