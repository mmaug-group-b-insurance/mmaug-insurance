package com.mmaug.claimsagent.validation;
import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope;
import com.mmaug.claimsagent.extraction.ExtractionStatus;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PolicyEvidenceValidatorTest {
 private final ValidationFindingRepository repo=mock(ValidationFindingRepository.class);
 private final PolicyEvidenceValidator validator=new PolicyEvidenceValidator(repo);
 private AgentRun run(){var c=new Claim();c.setId(UUID.randomUUID());var r=new AgentRun();r.setId(UUID.randomUUID());r.setClaim(c);return r;}
 private PolicyView policy(){return PolicyView.builder().policyNumber("SYN-1").startDate(LocalDate.of(2026,1,1)).endDate(LocalDate.of(2026,12,31)).vehicles(List.of(PolicyView.VehicleView.builder().registration("ABC-123").build())).build();}
 private ExtractionResponse evidence(String policy,String vehicle,String date){return ExtractionResponse.builder().extractionId(UUID.randomUUID()).documentId(UUID.randomUUID()).status(ExtractionStatus.SUCCEEDED).fields(List.of(field("policyNumber",policy),field("vehicleRegistration",vehicle),field("incidentDate",date))).build();}
 private ExtractionResponse.ExtractedFieldDto field(String name,String value){return ExtractionResponse.ExtractedFieldDto.builder().fieldName(name).value(value).page(2).snippet(value).build();}
 @Test void discrepanciesPreserveDocumentAndToolReferences(){var evidence=evidence("OTHER","ZZZ999","2027-01-01");assertThat(validator.validate(run(),ToolEnvelope.ok(policy(),"tool:policy-snapshot","v1"),List.of(evidence))).isEqualTo(3);var capture=org.mockito.ArgumentCaptor.forClass(List.class);verify(repo).saveAll(capture.capture());for(Object item:capture.getValue()){var f=(ValidationFinding)item;assertThat(f.getEvidenceRefs()).contains(evidence.getDocumentId().toString(),evidence.getExtractionId().toString(),"page:2","tool:policy-snapshot");assertThat(f.getResolved()).isFalse();}}
 @Test void matchingValuesAndInclusivePeriodBoundariesDoNotFlag(){assertThat(validator.validate(run(),ToolEnvelope.ok(policy(),"source","v1"),List.of(evidence("SYN-1","abc 123","2026-01-01"),evidence("SYN-1","ABC123","2026-12-31")))).isZero();}
 @Test void unavailablePolicyDoesNotInventMismatch(){assertThat(validator.validate(run(),ToolEnvelope.error("UNAVAILABLE","Unavailable","source","v1"),List.of(evidence("OTHER","ZZZ","2027-01-01")))).isZero();verifyNoInteractions(repo);}
 @Test void unknownDatesAndVehicleListDoNotBecomeMismatch(){var p=policy();p.setVehicles(null);p.setStartDate(null);assertThat(validator.validate(run(),ToolEnvelope.ok(p,"source","v1"),List.of(evidence("SYN-1","ZZZ","not a date")))).isZero();}
}
