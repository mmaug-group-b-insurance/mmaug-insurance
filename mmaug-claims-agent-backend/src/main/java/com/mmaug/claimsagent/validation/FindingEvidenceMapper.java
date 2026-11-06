package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.agent.summary.dto.EvidenceRefResponse;
import com.mmaug.claimsagent.agent.tools.ToolCallRepository;
import com.mmaug.claimsagent.document.ClaimDocumentRepository;
import com.mmaug.claimsagent.extraction.ExtractionResultRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.regex.Pattern;

/** Converts stored provenance into the established UI contract without rewriting evidence. */
@Component
public class FindingEvidenceMapper {
 private static final Pattern DOCUMENT=Pattern.compile("^document:([a-fA-F0-9-]{36}):extraction:([a-fA-F0-9-]{36}):page:([0-9]+):value:(.*)$",Pattern.DOTALL);
 private final ClaimDocumentRepository documents;
 private final ExtractionResultRepository extractions;
 private final ToolCallRepository calls;
 public FindingEvidenceMapper(ClaimDocumentRepository documents,ExtractionResultRepository extractions,ToolCallRepository calls){this.documents=documents;this.extractions=extractions;this.calls=calls;}
 public List<EvidenceRefResponse> map(ValidationFinding finding,List<String> refs){
  List<EvidenceRefResponse> out=new ArrayList<>();
  for(String ref:refs){
   var match=DOCUMENT.matcher(ref);
   if(match.matches()){
    try{
     var doc=documents.findById(UUID.fromString(match.group(1))).orElse(null);
     var extraction=extractions.findById(UUID.fromString(match.group(2))).orElse(null);
     int page=Integer.parseInt(match.group(3));
     if(doc==null||doc.getDeletedAt()!=null||!doc.getClaim().getId().equals(finding.getClaim().getId())||extraction==null||!extraction.getDocument().getId().equals(doc.getId())||page<1)continue;
     out.add(EvidenceRefResponse.builder().sourceType("DOCUMENT").documentId(doc.getId()).extractionId(extraction.getId()).documentName(doc.getOriginalFilename()).page(page).snippet(match.group(4)).fieldName(finding.getFieldName()).build());
    }catch(IllegalArgumentException e){ /* Invalid references remain available in evidenceRefs. */ }
   }else if(finding.getAgentRun()!=null && ref.startsWith("tool:")){
    for(var call:calls.findAllByRunIdOrderByCalledAtAsc(finding.getAgentRun().getId())){
     if(!call.getRun().getClaim().getId().equals(finding.getClaim().getId()))continue;
     try{
      var envelope=JsonMapper.builder().build().readTree(call.getResultJson());
      String source=envelope.path("sourceRef").asText();
      if(!source.isBlank() && (ref.equals(source)||ref.equals(source+":policySnapshot"))){
       out.add(EvidenceRefResponse.builder().sourceType("TOOL_RESULT").toolCallId(call.getId()).toolName(call.getToolName()).fieldName(finding.getFieldName()).build());break;
      }
     }catch(RuntimeException e){ /* Malformed snapshots do not become fabricated citations. */ }
    }
   }
  }
  return out;
 }
}
