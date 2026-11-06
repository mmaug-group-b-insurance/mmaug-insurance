package com.mmaug.claimsagent.agent.summary;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.guardrails.EvidenceVerifier;
import com.mmaug.claimsagent.agent.tools.*;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.math.BigDecimal;

/** Grounded deterministic draft. No external language model or unsupported narrative. */
@Service
public class GroundedSummaryBuilder {
 private final EvidenceVerifier verifier;
 private final ToolCallRepository toolCalls;
 private final EvidenceRefRepository evidence;
 public GroundedSummaryBuilder(EvidenceVerifier verifier,ToolCallRepository toolCalls,EvidenceRefRepository evidence){this.verifier=verifier;this.toolCalls=toolCalls;this.evidence=evidence;}
 public record Draft(String headline,String overallConfidence,List<GroundedStatement> statements,List<String> uncertaintyNotes){}
 public Draft build(AgentRun run,List<ExtractionResponse> extractions){
  List<GroundedStatement> candidates=new ArrayList<>();List<String> notes=new ArrayList<>();
  Map<String,Set<String>> values=new LinkedHashMap<>();
  for(var extraction:extractions){
   if(Boolean.TRUE.equals(extraction.getInjectionSuspected())||extraction.getFields()==null)continue;
   for(var field:extraction.getFields())if(field.getValue()!=null&&!field.getValue().isBlank())values.computeIfAbsent(field.getFieldName(),k->new LinkedHashSet<>()).add(field.getValue());
  }
  values.forEach((name,alternatives)->{if(alternatives.size()>1)notes.add("Conflicting document values retained for "+name+"; human review required");});
  for(String required:List.of("policyNumber","vehicleRegistration","incidentDate","incidentLocation","damageDescription"))
   if(!values.containsKey(required))notes.add("No usable document value for "+required+"; remains unknown");
  for(var extraction:extractions){
   if(Boolean.TRUE.equals(extraction.getInjectionSuspected())){notes.add("Instruction-like evidence excluded from factual summary: "+extraction.getDocumentId());continue;}
   if(extraction.getFields()==null)continue;
   for(var field:extraction.getFields()){
    if(field.getValue()==null||field.getValue().isBlank())continue;
    boolean conflict=values.getOrDefault(field.getFieldName(),Set.of()).size()>1;
    var citation=new GroundedStatement.Citation(SourceType.DOCUMENT,extraction.getDocumentId(),extraction.getExtractionId(),field.getPage(),field.getSnippet(),null,field.getFieldName(),field.getValue());
    candidates.add(new GroundedStatement("INCIDENT",conflict?StatementType.UNCERTAIN:StatementType.EXTRACTED_FACT,GroundedStatement.documentText(field.getFieldName(),field.getValue(),conflict),field.getConfidence(),List.of(citation)));
   }
  }
  for(var call:toolCalls.findAllByRunIdOrderByCalledAtAsc(run.getId())){
   if(call.getStatus()!=ToolCallStatus.SUCCESS){notes.add("Lookup "+call.getToolName()+" outcome: "+call.getStatus());continue;}
   try{
    var result=JsonMapper.builder().build().readTree(call.getResultJson());
    var fields=ToolSummaryFields.values(call.getToolName(),result.path("data"));
    if(fields.isEmpty())notes.add("get_business_rules".equals(call.getToolName()) ? "Synthetic rule snapshot is recorded in the analysis run; only high-value and late-reporting thresholds are applied from it. Full rule validation remains pending." : "No supported summary fields in lookup "+call.getToolName());
    fields.forEach((field,value)->{
     var citation=new GroundedStatement.Citation(SourceType.TOOL_RESULT,null,null,null,value,call.getId(),field,value);
     candidates.add(new GroundedStatement("POLICY",StatementType.RETRIEVED_FACT,GroundedStatement.toolText(call.getToolName(),field,value),null,List.of(citation)));
    });
   }catch(RuntimeException e){notes.add("Malformed lookup snapshot excluded: "+call.getToolName());}
  }
  List<GroundedStatement> verified=new ArrayList<>();
  for(var candidate:candidates){if(verifier.verify(run,candidate))verified.add(candidate);else notes.add("Unsupported statement excluded for "+candidate.citations().get(0).fieldName());}
  if(verified.isEmpty())throw new IllegalStateException("No source-supported summary statements available");
  notes.add("Deterministic source quotations; no language model was called. Field confidence is an uncalibrated parser heuristic.");
  notes.add("Coverage record counts do not establish eligibility. Historical counts use a five-year lookback at lookup time and do not indicate fraud or liability.");
  notes.add("Missing fields remain unknown; citation verification confirms recorded source support, not source truth or OCR accuracy.");
  return new Draft("Evidence preparation for claim "+run.getClaim().getClaimNumber(),"LOW",List.copyOf(verified),List.copyOf(notes));
 }
 public void persistEvidence(SummaryStatement statement,List<GroundedStatement.Citation> citations){
  for(var ref:citations){EvidenceRef saved=new EvidenceRef();saved.setStatement(statement);saved.setSourceType(ref.sourceType());saved.setDocumentId(ref.documentId());saved.setExtractionId(ref.extractionId());saved.setPageNumber(ref.page());saved.setSnippet(ref.snippet());saved.setToolCallId(ref.toolCallId());saved.setFieldName(ref.fieldName());evidence.save(saved);}
 }
}
