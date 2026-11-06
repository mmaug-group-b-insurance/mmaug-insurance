package com.mmaug.claimsagent.agent.guardrails;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.summary.*;
import com.mmaug.claimsagent.agent.tools.*;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.document.*;
import com.mmaug.claimsagent.extraction.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenceVerifierTest {
 private final ExtractionResultRepository extractions=mock(ExtractionResultRepository.class);
 private final ExtractedFieldRepository fields=mock(ExtractedFieldRepository.class);
 private final ToolCallRepository tools=mock(ToolCallRepository.class);
 private final EvidenceVerifier verifier=new EvidenceVerifier(extractions,fields,tools,new ForbiddenClaimsFilter());
 private final Claim claim=new Claim();private final AgentRun run=new AgentRun();private final ClaimDocument doc=new ClaimDocument();private final ExtractionResult extraction=new ExtractionResult();
 private GroundedStatement document(){
  claim.setId(UUID.randomUUID());run.setId(UUID.randomUUID());run.setClaim(claim);claim.setPolicyNumberStated("POL-MT-SYN001");
  doc.setId(UUID.randomUUID());doc.setClaim(claim);doc.setScanStatus(ScanStatus.CLEAN);
  extraction.setId(UUID.randomUUID());extraction.setDocument(doc);extraction.setStatus(ExtractionStatus.SUCCEEDED);
  ExtractedField f=new ExtractedField();f.setDocument(doc);f.setExtraction(extraction);f.setFieldName("vehicleRegistration");f.setFieldValue("SYN123");f.setSnippet("SYN123");f.setPageNumber(2);
  when(extractions.findById(extraction.getId())).thenReturn(Optional.of(extraction));when(fields.findAllByExtractionId(extraction.getId())).thenReturn(List.of(f));
  return new GroundedStatement("INCIDENT",StatementType.EXTRACTED_FACT,GroundedStatement.documentText("vehicleRegistration","SYN123",false),BigDecimal.valueOf(.75),List.of(new GroundedStatement.Citation(SourceType.DOCUMENT,doc.getId(),extraction.getId(),2,"SYN123",null,"vehicleRegistration","SYN123")));
 }
 @Test void verifiesExactPersistedDocumentField(){assertThat(verifier.verify(run,document())).isTrue();}
 @Test void statementWithRealCitationButInventedAssertionFails(){var source=document();var altered=new GroundedStatement(source.section(),source.type(),"The policy covers this accident",source.confidence(),source.citations());assertThat(verifier.verify(run,altered)).isFalse();}
 @Test void crossClaimReferenceFails(){var statement=document();Claim other=new Claim();other.setId(UUID.randomUUID());doc.setClaim(other);assertThat(verifier.verify(run,statement)).isFalse();}
 @Test void deletedQuarantinedAndInjectionEvidenceFail(){var statement=document();doc.setDeletedAt(java.time.Instant.now());assertThat(verifier.verify(run,statement)).isFalse();doc.setDeletedAt(null);doc.setScanStatus(ScanStatus.PENDING);assertThat(verifier.verify(run,statement)).isFalse();doc.setScanStatus(ScanStatus.CLEAN);extraction.setInjectionSuspected(true);assertThat(verifier.verify(run,statement)).isFalse();}
 @Test void wrongPageFails(){var statement=document();var c=statement.citations().get(0);var altered=new GroundedStatement(statement.section(),statement.type(),statement.text(),statement.confidence(),List.of(new GroundedStatement.Citation(c.sourceType(),c.documentId(),c.extractionId(),3,c.snippet(),null,c.fieldName(),c.sourceValue())));assertThat(verifier.verify(run,altered)).isFalse();}
 @Test void missingCitationFails(){var statement=document();assertThat(verifier.verify(run,new GroundedStatement(statement.section(),statement.type(),statement.text(),null,List.of()))).isFalse();}
 @Test void toolMustBelongToExactRunAndClaim(){document();ToolCall call=new ToolCall();call.setId(UUID.randomUUID());call.setRun(run);call.setToolName("get_policy");call.setStatus(ToolCallStatus.SUCCESS);call.setResultJson("{\"status\":\"OK\",\"data\":{\"policyNumber\":\"POL-MT-SYN001\",\"status\":\"ACTIVE\"}}");when(tools.findById(call.getId())).thenReturn(Optional.of(call));var statement=new GroundedStatement("POLICY",StatementType.RETRIEVED_FACT,GroundedStatement.toolText("get_policy","status","ACTIVE"),null,List.of(new GroundedStatement.Citation(SourceType.TOOL_RESULT,null,null,null,"ACTIVE",call.getId(),"status","ACTIVE")));assertThat(verifier.verify(run,statement)).isTrue();AgentRun other=new AgentRun();other.setId(UUID.randomUUID());other.setClaim(claim);call.setRun(other);assertThat(verifier.verify(run,statement)).isFalse();}
}
