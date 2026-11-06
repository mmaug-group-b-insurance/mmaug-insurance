package com.mmaug.claimsagent.agent.summary;
import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.guardrails.EvidenceVerifier;
import com.mmaug.claimsagent.agent.tools.ToolCallRepository;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GroundedSummaryBuilderTest {
 private final EvidenceVerifier verifier=mock(EvidenceVerifier.class);private final ToolCallRepository tools=mock(ToolCallRepository.class);private final EvidenceRefRepository refs=mock(EvidenceRefRepository.class);
 private final GroundedSummaryBuilder builder=new GroundedSummaryBuilder(verifier,tools,refs);
 private AgentRun run(){var r=new AgentRun();r.setId(UUID.randomUUID());var c=new Claim();c.setId(UUID.randomUUID());c.setClaimNumber("SYNTHETIC");r.setClaim(c);return r;}
 private ExtractionResponse extraction(String value){return ExtractionResponse.builder().documentId(UUID.randomUUID()).extractionId(UUID.randomUUID()).fields(List.of(ExtractionResponse.ExtractedFieldDto.builder().fieldName("vehicleRegistration").value(value).page(1).snippet(value).build())).build();}
 @Test void conflictingValuesRemainSeparateSourceLinkedStatements(){when(verifier.verify(any(),any())).thenReturn(true);var first=extraction("AAA123");var second=extraction("BBB456");var draft=builder.build(run(),List.of(first,second));assertThat(draft.statements()).hasSize(2).allSatisfy(st->assertThat(st.type()).isEqualTo(StatementType.UNCERTAIN));assertThat(draft.statements().get(0).citations().get(0).extractionId()).isEqualTo(first.getExtractionId());assertThat(draft.statements().get(1).citations().get(0).extractionId()).isEqualTo(second.getExtractionId());assertThat(draft.uncertaintyNotes()).anyMatch(n->n.contains("Conflicting document values"));}
 @Test void noVerifiedStatementCannotProduceSummary(){assertThatThrownBy(()->builder.build(run(),List.of(extraction("AAA123")))).isInstanceOf(IllegalStateException.class).hasMessageContaining("No source-supported");verifyNoInteractions(refs);}
 @Test void injectionFlaggedEvidenceCannotEnterCandidateStatements(){var input=extraction("AAA123");input.setInjectionSuspected(true);assertThatThrownBy(()->builder.build(run(),List.of(input))).isInstanceOf(IllegalStateException.class);verifyNoInteractions(verifier);}
 @Test void evidencePersistencePinsExactExtractionAndStatement(){var input=extraction("AAA123");var st=new SummaryStatement();var citation=new GroundedStatement.Citation(SourceType.DOCUMENT,input.getDocumentId(),input.getExtractionId(),1,"AAA123",null,"vehicleRegistration","AAA123");builder.persistEvidence(st,List.of(citation));var capture=org.mockito.ArgumentCaptor.forClass(EvidenceRef.class);verify(refs).save(capture.capture());assertThat(capture.getValue().getExtractionId()).isEqualTo(input.getExtractionId());assertThat(capture.getValue().getStatement()).isSameAs(st);}
}
