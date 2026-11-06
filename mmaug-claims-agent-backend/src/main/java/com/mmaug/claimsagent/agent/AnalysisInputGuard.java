package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.document.*;
import com.mmaug.claimsagent.common.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class AnalysisInputGuard {
 private final AgentRunRepository runs;private final ClaimRepository claims;private final ClaimDocumentRepository documents;
 public AnalysisInputGuard(AgentRunRepository runs,ClaimRepository claims,ClaimDocumentRepository documents){this.runs=runs;this.claims=claims;this.documents=documents;}
 public record DocumentInput(UUID id,boolean needsExtraction){}
 public List<DocumentInput> capture(UUID id){
  var run=runs.findById(id).orElseThrow();var claim=claims.findForDocumentUpload(run.getClaim().getId()).orElseThrow();
  requireActive(run);
  var docs=documents.findAllByClaimIdAndDeletedAtIsNull(claim.getId());
  String snapshot=snapshot(claim.getVersion(),docs);
  if(run.getInputSnapshotSha256()!=null && !run.getInputSnapshotSha256().equals(HashUtil.sha256(snapshot)))throw new InputsChanged();
  run.setInputSnapshot(snapshot);run.setInputSnapshotSha256(HashUtil.sha256(snapshot));runs.save(run);
  return docs.stream().map(d->new DocumentInput(d.getId(),d.getScanStatus()==ScanStatus.CLEAN && d.getProcessingStatus()!=ProcessingStatus.EXTRACTED)).toList();
 }
 public void assertCurrent(AgentRun run){
  requireActive(run);
  var claim=claims.findById(run.getClaim().getId()).orElseThrow();
  String current=snapshot(claim.getVersion(),documents.findAllByClaimIdAndDeletedAtIsNull(claim.getId()));
  if(run.getInputSnapshotSha256()==null || !run.getInputSnapshotSha256().equals(HashUtil.sha256(current)))throw new InputsChanged();
 }
 private void requireActive(AgentRun run){
  if(run.getStatus()!=AgentRunStatus.RUNNING || run.getExpiresAt()==null || !run.getExpiresAt().isAfter(Instant.now()))throw new InputsChanged();
 }
 private String snapshot(Long version,List<ClaimDocument> docs){
  var root=new TreeMap<String,Object>();root.put("claimVersion",version);
  root.put("documents",docs.stream().sorted(Comparator.comparing(d->d.getId().toString())).map(d->{
   var row=new TreeMap<String,Object>();row.put("id",d.getId().toString());row.put("sha256",d.getSha256());row.put("scan",d.getScanStatus().name());row.put("contentType",d.getContentType());row.put("container",d.getBlobContainer());row.put("blob",d.getBlobName());if(d.getStorageEtag()!=null)row.put("storageEtag",d.getStorageEtag());return row;
  }).toList());
  return JsonMapper.builder().build().writeValueAsString(root);
 }
 public static class InputsChanged extends RuntimeException{public InputsChanged(){super("Analysis inputs changed or its reservation is no longer active");}}
}
