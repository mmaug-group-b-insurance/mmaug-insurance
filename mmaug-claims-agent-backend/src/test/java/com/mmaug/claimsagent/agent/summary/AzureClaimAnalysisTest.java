package com.mmaug.claimsagent.agent.summary;

import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureClaimAnalysisTest {
 private final HttpClient http=mock(HttpClient.class);
 private final JsonMapper json=JsonMapper.builder().build();
 private final AzureClaimAnalysis service=new AzureClaimAnalysis("https://synthetic.openai.azure.com","synthetic-key","test-deployment",http);
 private GroundedSummaryBuilder.Draft draft(){var c=new GroundedStatement.Citation(SourceType.DOCUMENT,UUID.randomUUID(),UUID.randomUUID(),1,"SYN123",null,"vehicleRegistration","SYN123");return new GroundedSummaryBuilder.Draft("Synthetic","LOW",List.of(new GroundedStatement("INCIDENT",StatementType.EXTRACTED_FACT,GroundedStatement.documentText("vehicleRegistration","SYN123",false),null,List.of(c)),new GroundedStatement("POLICY",StatementType.RETRIEVED_FACT,"Recorded lookup - example",null,List.of(c))),List.of("Deterministic source quotations; no language model was called.","Missing fields remain unknown"));}
 @SuppressWarnings("unchecked") private HttpResponse<String> response(int status,String body){HttpResponse<String> r=mock(HttpResponse.class);when(r.statusCode()).thenReturn(status);when(r.body()).thenReturn(body);return r;}
 private String completion(String plan){return json.writeValueAsString(Map.of("model","synthetic-provider-model","choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",plan))),"usage",Map.of("prompt_tokens",12,"completion_tokens",8)));}
 private void reply(String plan)throws Exception{var r=response(200,completion(plan));when(http.send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(r);}
 @Test void modelCanOrderAndHighlightButCannotRewriteFactsOrCitations()throws Exception{reply("{\"order\":[1,0],\"highlights\":[0]}");var original=draft();var result=service.organize(original);assertThat(result.model()).isEqualTo("azure-openai/test-deployment");assertThat(result.tokensIn()).isEqualTo(12);assertThat(result.draft().statements()).hasSize(3);assertThat(result.draft().statements().get(0).section()).isEqualTo("OVERVIEW");assertThat(result.draft().statements().get(0).text()).isEqualTo(original.statements().get(0).text());assertThat(result.draft().statements().get(0).citations()).isEqualTo(original.statements().get(0).citations());assertThat(result.draft().statements().get(1)).isSameAs(original.statements().get(1));assertThat(result.draft().uncertaintyNotes()).noneMatch(n->n.contains("no language model was called"));}
 @Test void missingOrDuplicateSourceIdsFailClosed()throws Exception{reply("{\"order\":[0,0],\"highlights\":[]}");assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_INVALID_PLAN");}
 @Test void inventedSourceIdFailsClosed()throws Exception{reply("{\"order\":[0,2],\"highlights\":[]}");assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_INVALID_PLAN");}
 @Test void arbitraryProseIsRejectedEvenWithValidPlan()throws Exception{reply("{\"order\":[0,1],\"highlights\":[],\"text\":\"claim approved\"}");assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_INVALID_PLAN");}
 @Test void deniedProviderDoesNotExposeBodyOrFallback()throws Exception{var r=response(403,"secret provider diagnostic");when(http.send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(r);assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_ACCESS_DENIED");verify(http,times(1)).send(any(),any());}
 @Test void refusalAndTruncationAreNotAccepted()throws Exception{var r=response(200,"{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}");when(http.send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(r);assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_REFUSED_OR_INCOMPLETE");}
 @Test void timeoutIsExplicitAndNotRetried()throws Exception{when(http.send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new HttpTimeoutException("private"));assertThatThrownBy(()->service.organize(draft())).hasMessage("SUMMARY_MODEL_TIMEOUT");verify(http,times(1)).send(any(),any());}
 @Test void arbitraryHostAndHttpAreRejected(){assertThatThrownBy(()->new AzureClaimAnalysis("https://other.example","key","deployment",http)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new AzureClaimAnalysis("http://example.openai.azure.com","key","deployment",http)).isInstanceOf(IllegalArgumentException.class);}
 @Test void statementLimitFailsBeforeNetwork(){var input=draft();var many=new GroundedSummaryBuilder.Draft(input.headline(),"LOW",Collections.nCopies(81,input.statements().get(0)),List.of());assertThatThrownBy(()->service.organize(many)).hasMessage("SUMMARY_MODEL_INPUT_LIMIT");verifyNoInteractions(http);}
}
