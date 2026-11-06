package com.mmaug.claimsagent.agent.guardrails;
import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.summary.*;
import com.mmaug.claimsagent.agent.tools.*;
import com.mmaug.claimsagent.document.ScanStatus;
import com.mmaug.claimsagent.extraction.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.Objects;
/** Resolves persisted sources and verifies support for the constrained statement template. */
@Component
public class EvidenceVerifier {
 private final ExtractionResultRepository extractions;
 private final ExtractedFieldRepository fields;
 private final ToolCallRepository tools;
 private final ForbiddenClaimsFilter filter;
 public EvidenceVerifier(ExtractionResultRepository extractions,ExtractedFieldRepository fields,ToolCallRepository tools,ForbiddenClaimsFilter filter){this.extractions=extractions;this.fields=fields;this.tools=tools;this.filter=filter;}
 public boolean verify(AgentRun run,GroundedStatement statement){
  if(run==null||run.getId()==null||run.getClaim()==null||statement==null||statement.text()==null||statement.text().length()>1500||filter.containsForbidden(statement.text())||statement.citations()==null||statement.citations().isEmpty())return false;
  try{
   for(var ref:statement.citations()){
    if(ref.sourceType()==SourceType.DOCUMENT){
     if(ref.extractionId()==null||ref.documentId()==null||ref.page()==null||ref.page()<1||ref.snippet()==null||ref.snippet().isBlank())return false;
     var extraction=extractions.findById(ref.extractionId()).orElse(null);
     if(extraction==null||extraction.getStatus()==ExtractionStatus.FAILED||extraction.getStatus()==ExtractionStatus.PENDING||Boolean.TRUE.equals(extraction.getInjectionSuspected()))return false;
     var doc=extraction.getDocument();
     if(!doc.getId().equals(ref.documentId())||!doc.getClaim().getId().equals(run.getClaim().getId())||doc.getDeletedAt()!=null||doc.getScanStatus()!=ScanStatus.CLEAN)return false;
     boolean supported=fields.findAllByExtractionId(ref.extractionId()).stream().anyMatch(f->Objects.equals(f.getFieldName(),ref.fieldName())&&Objects.equals(f.getFieldValue(),ref.sourceValue())&&Objects.equals(f.getPageNumber(),ref.page())&&Objects.equals(f.getSnippet(),ref.snippet())&&f.getDocument().getId().equals(ref.documentId()));
     if(!supported||!Objects.equals(statement.text(),GroundedStatement.documentText(ref.fieldName(),ref.sourceValue(),statement.type()==StatementType.UNCERTAIN)))return false;
     if(statement.type()!=StatementType.EXTRACTED_FACT&&statement.type()!=StatementType.UNCERTAIN)return false;
    }else if(ref.sourceType()==SourceType.TOOL_RESULT){
     if(ref.toolCallId()==null||statement.type()!=StatementType.RETRIEVED_FACT)return false;
     var call=tools.findById(ref.toolCallId()).orElse(null);
     if(call==null||call.getStatus()!=ToolCallStatus.SUCCESS||!call.getRun().getId().equals(run.getId())||!call.getRun().getClaim().getId().equals(run.getClaim().getId()))return false;
     var result=JsonMapper.builder().build().readTree(call.getResultJson());
     if(!"OK".equals(result.path("status").asText())||!Objects.equals(result.path("data").path("policyNumber").asText(),run.getClaim().getPolicyNumberStated()))return false;
     var expected=ToolSummaryFields.values(call.getToolName(),result.path("data")).get(ref.fieldName());
     if(expected==null||!expected.equals(ref.sourceValue())||!statement.text().equals(GroundedStatement.toolText(call.getToolName(),ref.fieldName(),expected)))return false;
    }else return false;
   }
   return true;
  }catch(RuntimeException e){return false;}
 }
}
