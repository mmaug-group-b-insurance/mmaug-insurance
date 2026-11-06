package com.mmaug.claimsagent.extraction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class SyntheticEvidenceAcceptanceTest {
 @Test void labelledSyntheticFixtureSetPreservesValuesUnknownsAndPageProvenance()throws Exception{
  var json=JsonMapper.builder().build();
  var fields=List.of("policyNumber","policyholderName","driverName","vehicleRegistration","incidentDate","incidentLocation","policeReportNumber","garageName","damageDescription","estimatedRepairCost","currency");
  var results=new ArrayList<Map<String,Object>>();int assertions=0;
  try(var input=getClass().getResourceAsStream("/extraction/synthetic-acceptance.json")){
   var set=json.readTree(input);
   for(var fixture:set.path("fixtures")){
    UUID doc=UUID.randomUUID();var facts=new StructuringService().structure(doc,fixture.path("text").asText());
    var actual=json.readTree(json.writeValueAsString(facts));
    assertThat(facts.getInjectionSuspected()).as(fixture.path("id").asText()).isEqualTo(fixture.path("injectionSuspected").asBoolean());
    for(String field:fields){
     var expected=fixture.path("expected").path(field);var value=actual.path(field);
     if(expected.isMissingNode()){assertThat(value.isNull()||value.isMissingNode()).as(fixture.path("id").asText()+" missing "+field).isTrue();}
     else{
      assertThat(value.path("value").asText()).isEqualTo(expected.path("value").asText());
      assertThat(value.path("page").asInt()).isEqualTo(expected.path("page").asInt());
      assertThat(value.path("sourceDocumentId").asText()).isEqualTo(doc.toString());
      assertThat(value.path("snippet").asText()).isEqualTo(expected.path("value").asText());
     }
     assertions++;
    }
    results.add(Map.of("fixture",fixture.path("id").asText(),"status","passed"));
   }
  }
  Path output=Path.of("target","synthetic-structuring-acceptance.json");Files.createDirectories(output.getParent());
  Files.writeString(output,json.writeValueAsString(Map.of("scope","Offline structuring regression; not live OCR/model accuracy or a holdout evaluation","fixtureCount",results.size(),"fieldChecks",assertions,"results",results)));
 }
}
