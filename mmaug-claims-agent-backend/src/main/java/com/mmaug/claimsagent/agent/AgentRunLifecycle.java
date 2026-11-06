package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/** Short independent transactions. Never invokes OCR, models or processing services. */
@Service
@Transactional(propagation=Propagation.REQUIRES_NEW)
public class AgentRunLifecycle {
 private final AgentRunRepository runs;
 private final ClaimRepository claims;
 private final JdbcTemplate jdbc;
 public AgentRunLifecycle(AgentRunRepository runs,ClaimRepository claims,JdbcTemplate jdbc){this.runs=runs;this.claims=claims;this.jdbc=jdbc;}
 public record Reservation(UUID runId,boolean created){}
 public Reservation reserve(UUID claimId,UUID actor,String correlation){return reserve(claimId,actor,correlation,null);}
 public Reservation reserve(UUID claimId,UUID actor,String correlation,String key){
  if(key!=null && !key.matches("[A-Za-z0-9._:-]{1,100}"))throw new com.mmaug.claimsagent.common.exception.BusinessValidationException("Idempotency-Key must contain 1-100 letters, digits, dots, underscores, colons or hyphens");
  // PostgreSQL transaction-scoped advisory lock serializes reservation only. The unique
  // partial index also protects against writers that do not take this advisory lock.
  jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))",claimId.toString());
  var claim=claims.findById(claimId).orElseThrow(()->new NotFoundException("Claim not found"));
  if(key!=null){
   var existing=jdbc.queryForList("select run_id from agent_run_request where claim_id=? and actor_id=? and request_key=?",UUID.class,claimId,actor,key);
   if(!existing.isEmpty())return new Reservation(existing.get(0),false);
  }
  var active=runs.findFirstByClaimIdAndStatusInOrderByCreatedAtDesc(claimId,List.of(AgentRunStatus.QUEUED,AgentRunStatus.RUNNING));
  if(active.isPresent()){bind(claimId,actor,key,active.get().getId());return new Reservation(active.get().getId(),false);}
  AgentRun run=new AgentRun();run.setClaim(claim);run.setTriggeredByUserId(actor);run.setCorrelationId(correlation);
  run.setPromptVersion("v1");run.setToolSchemaVersion("v1");run.setStatus(AgentRunStatus.QUEUED);
  run.setStartedAt(Instant.now());run.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
  runs.findFirstByClaimIdOrderByCreatedAtDesc(claimId).filter(r->r.getStatus()==AgentRunStatus.FAILED).ifPresent(r->run.setRetryOfRunId(r.getId()));
  UUID id=runs.saveAndFlush(run).getId();bind(claimId,actor,key,id);return new Reservation(id,true);
 }
 private void bind(UUID claimId,UUID actor,String key,UUID runId){
  if(key!=null)jdbc.update("insert into agent_run_request(claim_id,actor_id,request_key,run_id) values (?,?,?,?)",claimId,actor,key,runId);
 }
 public boolean start(UUID id){
  return jdbc.update("update agent_run set status='RUNNING',updated_at=now(),version=version+1 where id=? and status='QUEUED' and expires_at>now()",id)==1;
 }
 public void fail(UUID id){fail(id,"PROCESSING_ROLLED_BACK");}
 public void fail(UUID id,String code){
  if(!code.matches("[A-Z_]{1,40}"))code="PROCESSING_ROLLED_BACK";
  jdbc.update("update agent_run set status='FAILED',completed_at=now(),updated_at=now(),version=version+1,error_code=?,error_message='Analysis could not publish safely. Previously committed evidence is preserved; explicit retry is required.' where id=? and status in ('QUEUED','RUNNING')",code,id);
 }
 public int recoverExpired(){
  // Locked rows belong to a transaction still finishing; never wait behind it.
  // Incrementing version fences a worker that loaded an expired run before recovery.
  return jdbc.update("with expired as (select id from agent_run where status in ('QUEUED','RUNNING') and coalesce(expires_at,started_at + interval '30 minutes',created_at + interval '30 minutes') < now() for update skip locked) update agent_run r set status='FAILED',completed_at=now(),updated_at=now(),version=r.version+1,error_code='PROCESS_INTERRUPTED',error_message='The processing reservation expired. Execution may have been interrupted; explicit retry is required.' from expired e where r.id=e.id");
 }
 @Transactional(readOnly=true,propagation=Propagation.REQUIRES_NEW)
 public AgentRun get(UUID id){return runs.findById(id).orElseThrow(()->new NotFoundException("Run not found"));}
}
