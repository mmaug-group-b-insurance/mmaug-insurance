package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.common.DatabaseWork;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;

/** Accepts only messages from the restricted queue, never an HTTP body or model output. */
@Service
@ConditionalOnProperty(name="app.scanner.provider",havingValue="defender-queue")
public class DefenderScanResults {
    private final ClaimDocumentRepository documents;private final ClaimRepository claims;
    private final BlobStorageService blobs;private final DatabaseWork database;
    private final URI storage;private final String topic;
    public DefenderScanResults(ClaimDocumentRepository documents,ClaimRepository claims,BlobStorageService blobs,DatabaseWork database,
                               @Value("${app.storage.endpoint}") String storage,@Value("${app.scanner.expected-topic}") String topic){
        AzureBlobStorageService.requireEndpoint(storage,".blob.core.windows.net");
        if(topic==null || !topic.startsWith("/subscriptions/") || !topic.contains("/providers/Microsoft.EventGrid/topics/"))throw new IllegalArgumentException("Expected Defender Event Grid topic required");
        this.documents=documents;this.claims=claims;this.blobs=blobs;this.database=database;this.storage=URI.create(storage);this.topic=topic;
    }
    private record Event(String id,String container,String name,String etag,Instant time,String result){}

    /** False leaves message visible for retry (including event arriving before upload commit). */
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NEVER)
    public boolean consume(String body){
        if(body==null || body.length()>65536)return true;
        JsonNode root;
        try{root=JsonMapper.builder().build().readTree(body);}catch(RuntimeException e){return true;}
        if(root.isArray()){
            if(root.size()>10)return false;
            boolean complete=true;
            for(var item:root){if(item.isObject())complete=consume(item.toString()) && complete;}
            return complete;
        }
        Event event=parse(root);
        if(event==null)return true;
        var found=documents.findByBlobContainerAndBlobNameAndDeletedAtIsNull(event.container(),event.name());
        if(found.isEmpty())return false;
        var doc=found.get();
        if(!event.etag().equals(normalize(doc.getStorageEtag())))return true;
        // Network version check is outside the short publication transaction.
        if(event.result().equals("No threats found")){
            String current;
            try{current=blobs.version(event.container(),event.name());}catch(RuntimeException e){return false;}
            if(!event.etag().equals(normalize(current)))return true;
        }
        // Defender can soft-delete malicious blobs before this event is consumed.
        // Negative verdicts block the recorded version even if the object is no longer readable.
        var documentId=doc.getId();var claimId=doc.getClaim().getId();
        return database.write(()->{
            claims.findForDocumentUpload(claimId).orElseThrow();
            var locked=documents.findForExtraction(documentId).orElseThrow();
            if(locked.getDeletedAt()!=null || !event.etag().equals(normalize(locked.getStorageEtag()))
                    || event.time().isBefore(locked.getUploadedAt().minusSeconds(60)))return true;
            ScanStatus verdict=switch(event.result()){
                case "No threats found" -> ScanStatus.CLEAN;
                case "Malicious" -> ScanStatus.INFECTED;
                default -> ScanStatus.REJECTED;
            };
            // At-least-once delivery cannot re-promote a rejected/infected version. A later
            // malicious verdict always blocks even an earlier clean result for that version.
            if(locked.getScanStatus()!=ScanStatus.PENDING && verdict!=ScanStatus.INFECTED)return true;
            locked.setScanStatus(verdict);locked.setScanEventId(event.id());locked.setScanFinishedAt(event.time());
            locked.setScanResultCode(verdict==ScanStatus.CLEAN?"DEFENDER_CLEAN":verdict==ScanStatus.INFECTED?"DEFENDER_INFECTED":"DEFENDER_REJECTED");
            locked.setProcessingStatus(verdict==ScanStatus.CLEAN?ProcessingStatus.UPLOADED:verdict==ScanStatus.INFECTED?ProcessingStatus.FAILED:ProcessingStatus.QUARANTINED);
            documents.save(locked);return true;
        });
    }
    private Event parse(JsonNode root){
        try{
            if(!root.path("eventType").asText().equals("Microsoft.Security.MalwareScanningResult") || !root.path("topic").asText().equalsIgnoreCase(topic))return null;
            String id=root.path("id").asText();var data=root.path("data");URI uri=URI.create(data.path("blobUri").asText());
            if(id.isBlank() || id.length()>150 || !"https".equals(uri.getScheme()) || !storage.getHost().equals(uri.getHost()) || uri.getUserInfo()!=null || uri.getPort()!=-1 || uri.getQuery()!=null || uri.getFragment()!=null)return null;
            String[] path=uri.getPath().split("/",3);
            if(path.length!=3 || !path[1].equals("quarantine") || path[2].isBlank() || path[2].startsWith("extractions/") || java.util.Arrays.stream(path[2].split("/")).anyMatch(java.util.Set.of(".","..")::contains) || path[2].contains("\\"))return null;
            String etag=normalize(data.path("eTag").asText());Instant time=Instant.parse(data.path("scanFinishedTimeUtc").asText());
            if(etag.isBlank() || etag.length()>150 || time.isAfter(Instant.now().plusSeconds(60)))return null;
            return new Event(id,path[1],path[2],etag,time,data.path("scanResultType").asText());
        }catch(RuntimeException e){return null;}
    }
    static String normalize(String etag){return etag==null?"":etag.replace("\"","");}
}
