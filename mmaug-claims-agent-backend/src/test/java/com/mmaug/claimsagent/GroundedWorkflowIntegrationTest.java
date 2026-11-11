package com.mmaug.claimsagent;

import com.mmaug.claimsagent.extraction.DocumentIntelligenceClientWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP -> real services/Flyway/PostgreSQL/storage -> source-linked summary -> versioned review. */
@SpringBootTest(classes=ClaimsAgentApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GroundedWorkflowIntegrationTest {
 @TempDir static Path storage;
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate db;
 @Autowired com.mmaug.claimsagent.agent.AgentRunLifecycle lifecycle;
 @Autowired com.mmaug.claimsagent.extraction.ExtractionLeaseRecovery extractionRecovery;
 @Autowired com.mmaug.claimsagent.document.ClaimDocumentRepository scanDocuments;
 @Autowired com.mmaug.claimsagent.claim.ClaimRepository scanClaims;
 @Autowired com.mmaug.claimsagent.common.DatabaseWork scanDatabase;
 @MockitoBean JwtDecoder jwtDecoder;
 @MockitoBean DocumentIntelligenceClientWrapper ocr;
 @org.springframework.test.context.bean.override.mockito.MockitoSpyBean com.mmaug.claimsagent.agent.summary.DeterministicClaimAnalysis model;
 private static final java.util.concurrent.atomic.AtomicInteger CLIENTS=new java.util.concurrent.atomic.AtomicInteger();
 private final String clientAddress="192.0.2."+CLIENTS.incrementAndGet();
 private final JsonMapper json=JsonMapper.builder().build();
 @DynamicPropertySource static void resources(DynamicPropertyRegistry r){
  String nativeUrl=System.getenv("MMAUG_INTEGRATION_DB_URL");
  if(nativeUrl!=null&&!nativeUrl.matches("jdbc:postgresql://127\\.0\\.0\\.1:55432/mmaug_sprint_test_[a-f0-9]+"))throw new IllegalStateException("Only a uniquely named isolated sprint test database is allowed");
  r.add("spring.datasource.url",()->nativeUrl==null?"jdbc:tc:postgresql:16-alpine:///mmaug_grounded_test":nativeUrl);
  r.add("spring.datasource.driver-class-name",()->nativeUrl==null?"org.testcontainers.jdbc.ContainerDatabaseDriver":"org.postgresql.Driver");
  r.add("spring.datasource.username",()->nativeUrl==null?"test":System.getenv("MMAUG_INTEGRATION_DB_USER"));
  r.add("spring.datasource.password",()->nativeUrl==null?"test":System.getenv("MMAUG_INTEGRATION_DB_PASSWORD"));
  r.add("spring.flyway.locations",()->"classpath:db/migration,classpath:db/seed");
  r.add("app.storage.base-path",()->storage.toString());r.add("app.security.enabled",()->"true");r.add("app.docintel.provider",()->"stub");
  r.add("app.ai.summary-provider",()->"deterministic");
 }
 private org.springframework.test.web.servlet.request.RequestPostProcessor officer() {
  return officer("00000000-0000-0000-0000-000000000001");
 }
 private org.springframework.test.web.servlet.request.RequestPostProcessor officer(String oid) {
  return request -> {
   request.setRemoteAddr(clientAddress);
   return jwt().jwt(j -> j.subject("opaque-pairwise-subject").claim("oid", oid).claim("scp", "mmaug_insurance_app"))
           .authorities(new SimpleGrantedAuthority("ROLE_CLAIMS_OFFICER")).postProcessRequest(request);
  };
 }
 private JsonNode getJson(String url)throws Exception{return json.readTree(mvc.perform(get(url).with(officer())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 private String analyse(String claim)throws Exception{return json.readTree(mvc.perform(post("/api/v1/claims/"+claim+"/analyse").with(officer()).contentType("application/json").content("{}")).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).path("runId").asText();}
 private String freshClaim()throws Exception{
  String body=json.writeValueAsString(Map.of("policyNumber","POL-MT-100001","claimantName","Synthetic recovery test","claimantPhone","+12025550123","claimantEmail","recovery@example.invalid","vehicleRegistration","SYN123","incidentDate","2026-09-15","incidentLocation","Synthetic location","incidentDescription","Synthetic motor accidental damage","injuriesReported",false,"thirdPartyInvolved",false));
  return json.readTree(mvc.perform(post("/api/v1/claims").with(officer()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
 }
 @Test void newlyCreatedClaimReopensWithReadablePiiAndEncryptedDatabaseColumns()throws Exception{
  String claim=freshClaim();
  var reopened=getJson("/api/v1/claims/"+claim).path("claim");
  assertThat(reopened.path("claimantName").asText()).isEqualTo("Synthetic recovery test");
  assertThat(reopened.path("claimantPhone").asText()).isEqualTo("+12025550123");
  assertThat(reopened.path("claimantEmail").asText()).isEqualTo("recovery@example.invalid");
  var stored=db.queryForMap("select claimant_name, claimant_phone, claimant_email from claim where id=?::uuid",claim);
  assertThat(stored.get("claimant_name")).isNotEqualTo("Synthetic recovery test");
  assertThat(stored.get("claimant_phone")).isNotEqualTo("+12025550123");
  assertThat(stored.get("claimant_email")).isNotEqualTo("recovery@example.invalid");
  var listed=getJson("/api/v1/claims?claimNumber="+reopened.path("claimNumber").asText()).path("content");
  assertThat(listed.size()).isEqualTo(1);
  assertThat(listed.get(0).path("claimantName").asText()).isEqualTo("Synthetic recovery test");
 }
 @Test void onlyAssignedOfficerCanReopenCaseAndUseItsPrivateDocuments() throws Exception {
  String claim = freshClaim();
  String base = "/api/v1/claims/" + claim;
  uploadSynthetic(claim, 42);
  String document = getJson(base + "/documents").get(0).path("id").asText();
  String number = getJson(base).path("claim").path("claimNumber").asText();
  String assigned = UUID.randomUUID().toString();
  var colleague = officer(assigned);
  mvc.perform(get(base)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/claims").param("claimNumber", number).with(colleague))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
  for (String path : List.of(base, base + "/documents", base + "/summary", base + "/findings",
          base + "/reviews", base + "/agent-runs", "/api/v1/documents/" + document + "/download")) {
   mvc.perform(get(path).with(colleague)).andExpect(status().isForbidden());
  }
  mvc.perform(multipart(base + "/documents").file("file", new byte[]{1}).with(colleague))
          .andExpect(status().isForbidden());
  db.update("update claim set assigned_officer_id=null where id=?::uuid", claim);
  mvc.perform(get(base).with(officer())).andExpect(status().isOk());
  mvc.perform(get(base).with(colleague)).andExpect(status().isForbidden());
  db.update("update claim set assigned_officer_id=?::uuid where id=?::uuid", assigned, claim);
  mvc.perform(get(base).with(colleague)).andExpect(status().isOk());
  mvc.perform(get(base).with(officer())).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/documents/" + document + "/download").with(officer())).andExpect(status().isForbidden());
 }

 @Test void officerRequiresDelegatedScopeAndValidDirectoryIdentity() throws Exception {
  var role = new SimpleGrantedAuthority("ROLE_CLAIMS_OFFICER");
  mvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j.claim("oid", UUID.randomUUID().toString())).authorities(role)))
          .andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j.claim("scp", "mmaug_insurance_app")).authorities(role)))
          .andExpect(status().isForbidden());
 }

 @Test void seededDatasetMeetsRfpTwentyPolicyAndTwelveClaimCatalogue() {
  assertThat(db.queryForObject("select count(*) from customer where customer_number like 'CUS-00%'", Integer.class)).isEqualTo(20);
  assertThat(db.queryForObject("select count(*) from policy where policy_number like 'POL-MT-1000%'", Integer.class)).isEqualTo(20);
  assertThat(db.queryForObject("select count(*) from claim where claim_number between 'CLM-2026-000001' and 'CLM-2026-000012'", Integer.class)).isEqualTo(12);
 }

 private void uploadSynthetic(String claim,int marker)throws Exception{
  mvc.perform(multipart("/api/v1/claims/"+claim+"/documents").file(new MockMultipartFile("file","recovery-"+marker+".png","image/png",new byte[]{(byte)0x89,0x50,0x4e,0x47,(byte)marker})).with(officer())).andExpect(status().isCreated());
 }
 private String analyseKey(String claim,String key)throws Exception{return json.readTree(mvc.perform(post("/api/v1/claims/"+claim+"/analyse").header("Idempotency-Key",key).with(officer()).contentType("application/json").content("{}")).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString()).path("runId").asText();}
 @Test void trustedScanResultCommitsOnlyForPinnedVersionAndBlocksInfectedDownload()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,91);
  UUID document=db.queryForObject("select id from claim_document where claim_id=?::uuid",UUID.class,claim);
  String blob=db.queryForObject("select blob_name from claim_document where id=?",String.class,document);
  db.update("update claim_document set scan_status='PENDING',processing_status='QUARANTINED',storage_etag='version-one' where id=?",document);
  mvc.perform(get("/api/v1/documents/"+document+"/download").with(officer())).andExpect(status().isUnprocessableEntity());
  var storage=mock(com.mmaug.claimsagent.document.BlobStorageService.class);
  when(storage.version("quarantine",blob)).thenAnswer(inv->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return "version-one";});
  String topic="/subscriptions/synthetic/resourceGroups/synthetic/providers/Microsoft.EventGrid/topics/scans";
  var results=new com.mmaug.claimsagent.document.DefenderScanResults(scanDocuments,scanClaims,storage,scanDatabase,"https://syntheticstore.blob.core.windows.net",topic);
  var data=new java.util.LinkedHashMap<String,Object>(Map.of("blobUri","https://syntheticstore.blob.core.windows.net/quarantine/"+blob,"eTag","version-one","scanFinishedTimeUtc",java.time.Instant.now().toString(),"scanResultType","No threats found"));
  var event=new java.util.LinkedHashMap<String,Object>(Map.of("id","integration-scan","eventType","Microsoft.Security.MalwareScanningResult","topic",topic,"data",data));
  assertThat(results.consume(json.writeValueAsString(event))).isTrue();
  assertThat(db.queryForObject("select scan_status from claim_document where id=?",String.class,document)).isEqualTo("CLEAN");
  assertThat(db.queryForObject("select scan_event_id from claim_document where id=?",String.class,document)).isEqualTo("integration-scan");
  data.put("scanResultType","Malicious");results.consume(json.writeValueAsString(event));
  data.put("scanResultType","No threats found");results.consume(json.writeValueAsString(event));
  assertThat(db.queryForObject("select scan_status from claim_document where id=?",String.class,document)).isEqualTo("INFECTED");
  mvc.perform(get("/api/v1/documents/"+document+"/download").with(officer())).andExpect(status().isUnprocessableEntity());
  assertThat(getJson("/api/v1/agent-runs/"+analyse(claim)).path("status").asText()).isEqualTo("FAILED");verifyNoInteractions(ocr);
 }
 @Test void keyedReplayAfterCompletionDoesNotCreateAnotherRunOrProviderCall()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,31);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  String first=analyseKey(claim,"synthetic-key-1");assertThat(analyseKey(claim,"synthetic-key-1")).isEqualTo(first);
  verify(ocr,times(1)).analyse(any(),any());
  assertThat(db.queryForObject("select count(*) from agent_run where claim_id=?::uuid",Integer.class,claim)).isEqualTo(1);
  mvc.perform(post("/api/v1/claims/"+claim+"/analyse").header("Idempotency-Key","invalid key").with(officer())).andExpect(status().isUnprocessableEntity());
  var other=lifecycle.reserve(UUID.fromString(freshClaim()),UUID.fromString("00000000-0000-0000-0000-000000000001"),"different-claim","synthetic-key-1");
  assertThat(other.runId().toString()).isNotEqualTo(first);lifecycle.fail(other.runId());
 }
 @Test void concurrentStartReturnsCommittedActiveRunWithoutSecondOcr()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,11);
  var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  when(ocr.analyse(any(),any())).thenAnswer(inv->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();entered.countDown();if(!release.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test latch timeout");return DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1);});
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{
   var first=executor.submit(()->analyse(claim));assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   String active=db.queryForObject("select id from agent_run where claim_id=?::uuid and status='RUNNING'",String.class,claim);
   var duplicate=executor.submit(()->analyseKey(claim,"active-alias-key"));assertThat(duplicate.get(5,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(active);
   assertThat(getJson("/api/v1/agent-runs/"+active).path("status").asText()).isEqualTo("RUNNING");
   release.countDown();assertThat(first.get(10,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(active);
   assertThat(analyseKey(claim,"active-alias-key")).isEqualTo(active);
   verify(ocr,times(1)).analyse(any(),any());assertThat(db.queryForObject("select count(*) from agent_run where claim_id=?::uuid",Integer.class,claim)).isEqualTo(1);
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void processingRollbackLeavesDurableFailureAndExplicitRetryPreservesPriorSummary()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,21);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  String first=analyse(claim);String oldSummary=getJson("/api/v1/claims/"+claim+"/summary").path("summaryId").asText();
  uploadSynthetic(claim,22);
  String safeClaim=UUID.fromString(claim).toString();
  db.execute("create function test_summary_failure() returns trigger language plpgsql as $$ begin if new.claim_id = '"+safeClaim+"'::uuid then raise exception 'Synthetic summary publication failure'; end if; return new; end $$");
  db.execute("create trigger test_summary_failure before insert on claim_summary for each row execute function test_summary_failure()");
  String failed=analyse(claim);var run=getJson("/api/v1/agent-runs/"+failed);
  assertThat(run.path("status").asText()).isEqualTo("FAILED");assertThat(run.path("errorCode").asText()).isEqualTo("PROCESSING_ROLLED_BACK");
  assertThat(run.path("errorMessage").asText()).doesNotContain("division", "select", "password");
  assertThat(run.path("steps").size()).isGreaterThan(0);assertThat(run.path("toolCalls").size()).isEqualTo(4);
  assertThat(getJson("/api/v1/claims/"+claim+"/summary").path("summaryId").asText()).isEqualTo(oldSummary);
  assertThat(db.queryForObject("select latest_run_id from claim where id=?::uuid",String.class,claim)).isEqualTo(first);
  db.execute("drop trigger test_summary_failure on claim_summary");db.execute("drop function test_summary_failure()");
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  String retried=analyse(claim);var retry=getJson("/api/v1/agent-runs/"+retried);
  assertThat(retry.path("retryOfRunId").asText()).isEqualTo(failed);assertThat(retry.path("status").asText()).isNotEqualTo("FAILED");
  assertThat(getJson("/api/v1/claims/"+claim+"/summary?version=1").path("summaryId").asText()).isEqualTo(oldSummary);
 }
 @Test void recoveryFencesALateWorkerAndPreservesInterruptedStatus()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,41);
  var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  when(ocr.analyse(any(),any())).thenAnswer(inv->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();entered.countDown();if(!release.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test latch timeout");return DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1);});
  var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
  try{
   var worker=executor.submit(()->analyse(claim));assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   UUID active=UUID.fromString(db.queryForObject("select id from agent_run where claim_id=?::uuid and status='RUNNING'",String.class,claim));
   db.update("update agent_run set expires_at=now()-interval '1 minute' where id=?",active);
   assertThat(lifecycle.recoverExpired()).isGreaterThanOrEqualTo(1);
   release.countDown();assertThat(worker.get(10,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(active.toString());
   var run=getJson("/api/v1/agent-runs/"+active);assertThat(run.path("status").asText()).isEqualTo("FAILED");assertThat(run.path("errorCode").asText()).isEqualTo("PROCESS_INTERRUPTED");
   assertThat(db.queryForObject("select count(*) from claim_summary where claim_id=?::uuid",Integer.class,claim)).isZero();
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void modelWaitReleasesTransactionAndConcurrentUploadInvalidatesPublication()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,51);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  doAnswer(inv->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();entered.countDown();if(!release.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test timeout");return inv.callRealMethod();}).when(model).organize(any());
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{
   var worker=executor.submit(()->analyse(claim));assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   executor.submit(()->{uploadSynthetic(claim,52);return true;}).get(5,java.util.concurrent.TimeUnit.SECONDS);
   release.countDown();String runId=worker.get(10,java.util.concurrent.TimeUnit.SECONDS);
   var run=getJson("/api/v1/agent-runs/"+runId);assertThat(run.path("status").asText()).isEqualTo("FAILED");assertThat(run.path("errorCode").asText()).isEqualTo("INPUTS_CHANGED");
   assertThat(db.queryForObject("select count(*) from claim_summary where claim_id=?::uuid",Integer.class,claim)).isZero();
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void officerReviewDuringModelWaitRemainsDurableAndPreventsStalePublication()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,61);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  analyse(claim);String previous=getJson("/api/v1/claims/"+claim+"/summary").path("summaryId").asText();
  var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  doAnswer(inv->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();entered.countDown();if(!release.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test timeout");return inv.callRealMethod();}).when(model).organize(any());
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{
   var worker=executor.submit(()->analyse(claim));assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   executor.submit(()->mvc.perform(post("/api/v1/claims/"+claim+"/reviews").with(officer()).contentType("application/json").content(json.writeValueAsString(Map.of("summaryId",previous,"outcome","INFO_REQUESTED","comment","Synthetic concurrent review","infoRequested","Synthetic follow-up")))).andExpect(status().isCreated())).get(5,java.util.concurrent.TimeUnit.SECONDS);
   release.countDown();var run=getJson("/api/v1/agent-runs/"+worker.get(10,java.util.concurrent.TimeUnit.SECONDS));
   assertThat(run.path("errorCode").asText()).isEqualTo("INPUTS_CHANGED");
   assertThat(getJson("/api/v1/claims/"+claim+"/summary").path("summaryId").asText()).isEqualTo(previous);
   assertThat(getJson("/api/v1/claims/"+claim+"/reviews").toString()).contains(previous,"Synthetic follow-up");
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void documentDeletionDuringModelWaitCannotBecomePublishedEvidence()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,81);
  String document=db.queryForObject("select id from claim_document where claim_id=?::uuid",String.class,claim);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  doAnswer(inv->{entered.countDown();if(!release.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test timeout");return inv.callRealMethod();}).when(model).organize(any());
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{
   var worker=executor.submit(()->analyse(claim));assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   executor.submit(()->mvc.perform(delete("/api/v1/documents/"+document).with(officer())).andExpect(status().isNoContent())).get(5,java.util.concurrent.TimeUnit.SECONDS);
   release.countDown();var run=getJson("/api/v1/agent-runs/"+worker.get(10,java.util.concurrent.TimeUnit.SECONDS));
   assertThat(run.path("errorCode").asText()).isEqualTo("INPUTS_CHANGED");
   assertThat(db.queryForObject("select count(*) from claim_summary where claim_id=?::uuid",Integer.class,claim)).isZero();
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void explicitExtractionRetryRetiresExpiredPendingAttemptBeforeRecoverySweep()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,91);
  String document=db.queryForObject("select id from claim_document where claim_id=?::uuid",String.class,claim);UUID old=UUID.randomUUID();
  db.update("insert into extraction_result(id,document_id,status) values (?,?::uuid,'PENDING')",old,document);
  db.update("update claim_document set processing_status='EXTRACTING',extraction_token=?,extraction_expires_at=now()-interval '1 minute' where id=?::uuid",old,document);
  doReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001",1)).when(ocr).analyse(any(),any());
  mvc.perform(post("/api/v1/documents/"+document+"/extraction").with(officer())).andExpect(status().isAccepted());
  assertThat(db.queryForObject("select status from extraction_result where id=?",String.class,old)).isEqualTo("FAILED");
  assertThat(db.queryForObject("select count(*) from extraction_result where document_id=?::uuid and status='PENDING'",Integer.class,document)).isZero();
  assertThat(db.queryForObject("select processing_status from claim_document where id=?::uuid",String.class,document)).isEqualTo("EXTRACTED");
  verify(ocr,times(1)).analyse(any(),any());
 }
 @Test void expiredDocumentExtractionIsMarkedFailedWithoutProviderRetry()throws Exception{
  String claim=freshClaim();uploadSynthetic(claim,71);
  db.update("update claim_document set processing_status='EXTRACTING',extraction_token=gen_random_uuid(),extraction_expires_at=now()-interval '1 minute' where claim_id=?::uuid",claim);
  db.execute("insert into extraction_result(id,document_id,status) select extraction_token,id,'PENDING' from claim_document where claim_id='"+UUID.fromString(claim)+"'::uuid");
  assertThat(extractionRecovery.recoverExpired()).isEqualTo(1);assertThat(extractionRecovery.recoverExpired()).isZero();
  assertThat(db.queryForObject("select processing_status from claim_document where claim_id=?::uuid",String.class,claim)).isEqualTo("FAILED");
  assertThat(db.queryForObject("select error_code from extraction_result where document_id=(select id from claim_document where claim_id=?::uuid) order by created_at desc limit 1",String.class,claim)).isEqualTo("EXTRACTION_INTERRUPTED");
  verifyNoInteractions(ocr,model);
 }
 @Test void expiredReservationRecoveryIsIdempotentAndDoesNotInvokeProviders()throws Exception{
  String claim=freshClaim();UUID claimId=UUID.fromString(claim),actor=UUID.fromString("00000000-0000-0000-0000-000000000001");
  var reserved=lifecycle.reserve(claimId,actor,"synthetic-recovery");
  assertThat(lifecycle.reserve(claimId,actor,"duplicate").runId()).isEqualTo(reserved.runId());
  db.update("update agent_run set expires_at=now()-interval '1 minute' where id=?",reserved.runId());
  assertThat(lifecycle.recoverExpired()).isGreaterThanOrEqualTo(1);assertThat(lifecycle.recoverExpired()).isZero();
  var failed=getJson("/api/v1/agent-runs/"+reserved.runId());assertThat(failed.path("status").asText()).isEqualTo("FAILED");assertThat(failed.path("errorCode").asText()).isEqualTo("PROCESS_INTERRUPTED");
  assertThat(lifecycle.start(reserved.runId())).isFalse();verifyNoInteractions(ocr);
  var retry=lifecycle.reserve(claimId,actor,"explicit-retry");assertThat(retry.created()).isTrue();assertThat(lifecycle.get(retry.runId()).getRetryOfRunId()).isEqualTo(reserved.runId());
  lifecycle.fail(retry.runId());
 }
 @Test void openApiMatchesReviewedContractBaseline()throws Exception{
  String actualBody=mvc.perform(get("/v3/api-docs").with(officer())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  try(var stream=getClass().getResourceAsStream("/contracts/openapi-baseline.json")){
   assertThat(stream).as("Committed OpenAPI contract baseline exists").isNotNull();
   var expected=json.readTree(stream);
   var actual=json.readTree(actualBody);
   // The server URL is deployment-specific. All operation and schema content is compared.
   var actualMap=json.readValue(actualBody,java.util.LinkedHashMap.class);actualMap.remove("servers");
   assertThat(json.readTree(json.writeValueAsString(actualMap))).as("OpenAPI changed: review consumers and explicitly update contracts/openapi-baseline.json").isEqualTo(expected);
   assertThat(actual.path("components").path("schemas").path("FindingResponse").path("properties").path("evidence").path("items").path("$ref").asText()).isEqualTo("#/components/schemas/EvidenceRefResponse");
  }
 }
 @Test void sourceLinkedVersionsReviewsAndAccessChecks()throws Exception{
  when(ocr.analyse(any(),eq("image/png"))).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-100001\nVehicle Registration: ABC 123\nIncident Date: 2026-09-15\nLocation: Synthetic, quoted location\nDamage Description: Synthetic bumper damage",1));
  String claim=db.queryForObject("select id from claim where claim_number='CLM-2026-000001'",String.class);
  String base="/api/v1/claims/"+claim;
  var file=new MockMultipartFile("file","synthetic.png","application/octet-stream",new byte[]{(byte)0x89,0x50,0x4e,0x47,13,10,26,10});
  var upload=json.readTree(mvc.perform(multipart(base+"/documents").file(file).param("documentType","CLAIM_FORM").with(officer())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  assertThat(upload.path("contentType").asText()).isEqualTo("image/png");
  mvc.perform(multipart(base+"/documents").file(file).with(officer())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_DOCUMENT"));
  String runId=analyse(claim);var run=getJson("/api/v1/agent-runs/"+runId);
  assertThat(run.path("status").asText()).isNotEqualTo("FAILED");assertThat(run.path("toolCalls").size()).isEqualTo(4);
  assertThat(run.path("ruleSetVersion").asText()).isEqualTo("synthetic-1.0.0");
  assertThat(run.path("ruleSetSha256").asText()).matches("[a-f0-9]{64}");
  assertThat(run.path("inputSnapshotSha256").asText()).matches("[a-f0-9]{64}");
  var first=getJson(base+"/summary");assertThat(first.path("llmModel").asText()).isEqualTo("grounded-template-v2");
  List<JsonNode> statements=new ArrayList<>();for(var section:first.path("sections"))for(var statement:section)statements.add(statement);
  assertThat(statements).isNotEmpty();assertThat(statements).allSatisfy(st->assertThat(st.path("evidence").size()).isGreaterThan(0));
  assertThat(statements.stream().anyMatch(st->st.path("type").asText().equals("RETRIEVED_FACT"))).isTrue();
  assertThat(statements.stream().filter(st->st.path("type").asText().equals("EXTRACTED_FACT")).allMatch(st->!st.path("evidence").get(0).path("extractionId").isNull())).isTrue();
  String firstId=first.path("summaryId").asText();String original=statements.get(0).path("text").asText();
  String otherClaim=db.queryForObject("select id from claim where claim_number='CLM-2026-000002'",String.class);
  String forgedReview=json.writeValueAsString(Map.of("summaryId",firstId,"outcome","ACKNOWLEDGED","comment","Cross-claim attempt"));
  mvc.perform(post("/api/v1/claims/"+otherClaim+"/reviews").with(officer()).contentType("application/json").content(forgedReview)).andExpect(status().isForbidden());
  String secondRun=analyse(claim);var second=getJson(base+"/summary");assertThat(second.path("versionNo").asInt()).isEqualTo(2);
  assertThat(getJson(base+"/summary?version=1").path("summaryId").asText()).isEqualTo(firstId);
  verify(ocr,times(1)).analyse(any(),any());
  // Explicit synthetic fixtures distinguish current, historical, resolved and pre-run findings.
  String insert="insert into validation_finding(id,claim_id,agent_run_id,finding_type,severity,origin,message,resolved) values (?::uuid,?::uuid,?::uuid,'OTHER','HIGH','RULE_ENGINE','Synthetic selection fixture',?)";
  String oldFinding=UUID.randomUUID().toString(), currentFinding=UUID.randomUUID().toString(), resolvedFinding=UUID.randomUUID().toString(), legacyFinding=UUID.randomUUID().toString();
  db.update(insert,oldFinding,claim,runId,false);db.update(insert,currentFinding,claim,secondRun,false);db.update(insert,resolvedFinding,claim,secondRun,true);db.update(insert,legacyFinding,claim,null,false);
  var docRef=statements.stream().filter(st->st.path("type").asText().equals("EXTRACTED_FACT")).findFirst().orElseThrow().path("evidence").get(0);
  String documentRef="document:"+docRef.path("documentId").asText()+":extraction:"+docRef.path("extractionId").asText()+":page:"+docRef.path("page").asInt()+":value:"+docRef.path("snippet").asText();
  String toolRef="tool:get_policy:claim:"+claim+":run:"+secondRun+":policySnapshot";
  db.update("update validation_finding set evidence_refs=?::jsonb where id=?::uuid",json.writeValueAsString(List.of(documentRef,toolRef,"unknown:preserved")),currentFinding);
  var currentFindings=getJson(base+"/findings");
  assertThat(currentFindings.toString()).contains(currentFinding,resolvedFinding).doesNotContain(oldFinding,legacyFinding);
  for(var finding:currentFindings) assertThat(finding.path("runId").asText()).isEqualTo(secondRun);
  JsonNode renderedFinding=null;for(var finding:currentFindings)if(finding.path("id").asText().equals(currentFinding))renderedFinding=finding;
  assertThat(renderedFinding).isNotNull();assertThat(renderedFinding.path("evidence").size()).isEqualTo(2);
  assertThat(renderedFinding.path("evidence").get(0).path("sourceType").asText()).isEqualTo("DOCUMENT");
  assertThat(renderedFinding.path("evidence").get(0).path("documentName").asText()).isEqualTo("synthetic.png");
  assertThat(renderedFinding.path("evidence").get(1).path("sourceType").asText()).isEqualTo("TOOL_RESULT");
  assertThat(renderedFinding.path("evidence").get(1).path("toolCallId").asText()).matches("[a-f0-9-]{36}");
  assertThat(renderedFinding.path("evidenceRefs").toString()).contains("unknown:preserved");

  assertThat(getJson(base+"/findings?resolved=false").toString()).contains(currentFinding).doesNotContain(resolvedFinding);
  assertThat(getJson(base+"/findings?runId="+runId).toString()).contains(oldFinding).doesNotContain(currentFinding);
  assertThat(getJson(base+"/findings?includeHistory=true").toString()).contains(oldFinding,currentFinding,resolvedFinding,legacyFinding);
  mvc.perform(get("/api/v1/claims/"+otherClaim+"/findings?runId="+runId).with(officer())).andExpect(status().isForbidden());
  mvc.perform(get(base+"/findings?runId="+runId+"&includeHistory=true").with(officer())).andExpect(status().isUnprocessableEntity());
  mvc.perform(get(base+"/findings?runId="+UUID.randomUUID()).with(officer())).andExpect(status().isNotFound());
  long expectedOpen=db.queryForObject("select count(*) from validation_finding f join claim c on c.id=f.claim_id where f.resolved=false and (f.agent_run_id=c.latest_run_id or (f.agent_run_id is null and c.latest_run_id is null))",Long.class);
  assertThat(getJson("/api/v1/dashboard").path("openFindingsCount").asLong()).isEqualTo(expectedOpen);

  String stale=json.writeValueAsString(Map.of("summaryId",firstId,"outcome","ACKNOWLEDGED","comment","Synthetic stale review"));
  mvc.perform(post(base+"/reviews").with(officer()).contentType("application/json").content(stale)).andExpect(status().isUnprocessableEntity());
  var currentStatement=second.path("sections").iterator().next().get(0);
  String body=json.writeValueAsString(Map.of("summaryId",second.path("summaryId").asText(),"outcome","ACKNOWLEDGED","comment","Synthetic review, with commas and quotes: \"checked\"","edits",List.of(Map.of("statementId",currentStatement.path("id").asText(),"editedText","Officer annotation only","reason","Synthetic correction"))));
  mvc.perform(post(base+"/reviews").with(officer()).contentType("application/json").content(body)).andExpect(status().isCreated());
  assertThat(db.queryForObject("select text from summary_statement where id=?::uuid",String.class,currentStatement.path("id").asText())).isEqualTo(currentStatement.path("text").asText());
  assertThat(db.queryForObject("select count(*) from officer_edit",Integer.class)).isEqualTo(1);
  assertThat(db.queryForObject("select count(*) from officer_review where summary_id=?::uuid",Integer.class,second.path("summaryId").asText())).isEqualTo(1);
  assertThat(getJson(base+"/summary?version=1").toString()).contains(original);
  mvc.perform(get(base+"/summary")).andExpect(status().isUnauthorized());
  mvc.perform(get(base+"/summary").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT")))).andExpect(status().isForbidden());
  mvc.perform(get("/internal/tools/v1/business-rules?productCode=PRIVATE_MOTOR").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE_AGENT"))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.ruleSetVersion").value("synthetic-1.0.0"));
  mvc.perform(get("/internal/tools/v1/business-rules?productCode=OTHER").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE_AGENT")))).andExpect(status().isNotFound());
  mvc.perform(get("/internal/tools/v1/policies/POL-MT-100001").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE_AGENT")))) .andExpect(status().isOk()).andExpect(jsonPath("$.data.policyNumber").value("POL-MT-100001"));
 }
 @Test void failedOcrPersistsFailureWithoutSummaryOrFabricatedToolCalls()throws Exception{
  when(ocr.analyse(any(),any())).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.failed("OCR_UNAVAILABLE","Synthetic outage"));
  String claim=freshClaim();
  String base="/api/v1/claims/"+claim;
  var upload=json.readTree(mvc.perform(multipart(base+"/documents").file(new MockMultipartFile("file","outage.png","image/png",new byte[]{(byte)0x89,0x50,0x4e,0x47,1})).with(officer())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  var run=getJson("/api/v1/agent-runs/"+analyse(claim));
  assertThat(run.path("status").asText()).isEqualTo("FAILED");assertThat(run.path("toolCalls").size()).isZero();
  assertThat(getJson("/api/v1/documents/"+upload.path("id").asText()+"/extraction").path("status").asText()).isEqualTo("FAILED");
  mvc.perform(get(base+"/summary").with(officer())).andExpect(status().isNotFound());
 }
 @Test void quarantinedEvidenceNeverReachesOcrOrSummary()throws Exception{
  String claim=freshClaim();
  String base="/api/v1/claims/"+claim;
  var upload=json.readTree(mvc.perform(multipart(base+"/documents").file(new MockMultipartFile("file","quarantine.png","image/png",new byte[]{(byte)0x89,0x50,0x4e,0x47,2})).with(officer())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  db.update("update claim_document set scan_status='PENDING',processing_status='QUARANTINED' where id=?::uuid",upload.path("id").asText());
  var run=getJson("/api/v1/agent-runs/"+analyse(claim));assertThat(run.path("status").asText()).isEqualTo("FAILED");verifyNoInteractions(ocr);
  mvc.perform(get(base+"/summary").with(officer())).andExpect(status().isNotFound());
 }
 @Test void noMatchingPolicyPreservesDocumentFactsWithoutInventedPolicyHistory()throws Exception{
  String body=json.writeValueAsString(Map.of("policyNumber","POL-MT-999999","claimantName","Synthetic no-match","claimantPhone","+12025550123","claimantEmail","test@example.invalid","vehicleRegistration","SYN123","incidentDate","2026-09-15","incidentLocation","Synthetic location","incidentDescription","Synthetic motor accidental damage only","injuriesReported",false,"thirdPartyInvolved",false));
  String claim=json.readTree(mvc.perform(post("/api/v1/claims").with(officer()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
  when(ocr.analyse(any(),any())).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: POL-MT-999999\nVehicle Registration: SYN123",1));
  String base="/api/v1/claims/"+claim;
  mvc.perform(multipart(base+"/documents").file(new MockMultipartFile("file","no-match.png","image/png",new byte[]{(byte)0x89,0x50,0x4e,0x47,3})).with(officer())).andExpect(status().isCreated());
  var run=getJson("/api/v1/agent-runs/"+analyse(claim));
  assertThat(run.path("status").asText()).isNotEqualTo("FAILED");
  for(var call:run.path("toolCalls")){assertThat(call.path("status").asText()).isEqualTo("NOT_FOUND");assertThat(call.path("resultEnvelope").path("data").isNull()).isTrue();}
  var summary=getJson(base+"/summary");assertThat(summary.path("sections").has("POLICY")).isFalse();
  assertThat(summary.path("uncertaintyNotes").toString()).contains("NOT_FOUND","remains unknown");
 }
}
