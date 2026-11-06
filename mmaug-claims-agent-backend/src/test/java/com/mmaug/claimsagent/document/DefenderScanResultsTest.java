package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.common.DatabaseWork;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefenderScanResultsTest {
    private final ClaimDocumentRepository documents=mock(ClaimDocumentRepository.class);
    private final ClaimRepository claims=mock(ClaimRepository.class);
    private final BlobStorageService blobs=mock(BlobStorageService.class);
    private final DatabaseWork database=mock(DatabaseWork.class);
    private final String topic="/subscriptions/synthetic/resourceGroups/synthetic/providers/Microsoft.EventGrid/topics/scans";
    private final Claim claim=new Claim();private final ClaimDocument doc=new ClaimDocument();
    private final DefenderScanResults service=new DefenderScanResults(documents,claims,blobs,database,"https://syntheticstore.blob.core.windows.net",topic);
    DefenderScanResultsTest(){
        claim.setId(UUID.randomUUID());doc.setId(UUID.randomUUID());doc.setClaim(claim);doc.setBlobContainer("quarantine");doc.setBlobName("synthetic.png");
        doc.setStorageEtag("\"version-one\"");doc.setUploadedAt(Instant.now().minusSeconds(10));doc.setProcessingStatus(ProcessingStatus.QUARANTINED);
        when(documents.findByBlobContainerAndBlobNameAndDeletedAtIsNull("quarantine","synthetic.png")).thenReturn(Optional.of(doc));
        when(documents.findForExtraction(doc.getId())).thenReturn(Optional.of(doc));when(claims.findForDocumentUpload(claim.getId())).thenReturn(Optional.of(claim));
        when(blobs.version("quarantine","synthetic.png")).thenReturn("\"version-one\"");
        when(database.write(any())).thenAnswer(i->((java.util.function.Supplier<?>)i.getArgument(0)).get());
    }
    private String event(String result,String etag,String host,String eventTopic){
        return JsonMapper.builder().build().writeValueAsString(Map.of("id","synthetic-event","eventType","Microsoft.Security.MalwareScanningResult","topic",eventTopic,
                "data",Map.of("blobUri","https://"+host+"/quarantine/synthetic.png","eTag",etag,"scanFinishedTimeUtc",Instant.now().toString(),"scanResultType",result)));
    }
    private String event(String result){return event(result,"version-one","syntheticstore.blob.core.windows.net",topic);}
    @Test void uploadStaysPendingUntilTrustedVersionMatchedResult(){
        assertThat(new DefenderScanGate().scan("quarantine","synthetic.png")).isEqualTo(ScanStatus.PENDING);
        assertThat(service.consume(event("No threats found"))).isTrue();
        assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.CLEAN);assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        assertThat(doc.getScanEventId()).isEqualTo("synthetic-event");assertThat(doc.getScanFinishedAt()).isNotNull();
    }
    @Test void maliciousAndFailedResultsNeverClear(){
        when(blobs.version(anyString(),anyString())).thenThrow(new IllegalStateException("Defender already quarantined blob"));
        assertThat(service.consume(event("Malicious"))).isTrue();assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.INFECTED);
        assertThat(service.consume(event("No threats found"))).isFalse();assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.INFECTED);
    }
    @Test void errorsUnknownAndUnsupportedStayQuarantined(){
        for(String result:List.of("Error","Not scanned","unknown","")){
            doc.setScanStatus(ScanStatus.PENDING);service.consume(event(result));
            assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.REJECTED);assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.QUARANTINED);
        }
    }
    @Test void wrongTopicAndHostAreDiscardedBeforeStorage(){
        service.consume(event("No threats found","version-one","attacker.blob.core.windows.net",topic));
        service.consume(event("No threats found","version-one","syntheticstore.blob.core.windows.net",topic+"-other"));
        verifyNoInteractions(documents,blobs,database);
    }
    @Test void wrongUploadOrCurrentBlobVersionNeverPublishesClean(){
        service.consume(event("No threats found","old-version","syntheticstore.blob.core.windows.net",topic));verifyNoInteractions(blobs,database);
        when(blobs.version(anyString(),anyString())).thenReturn("overwritten-version");service.consume(event("No threats found"));
        assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.PENDING);verifyNoInteractions(database);
    }
    @Test void missingDocumentAndUnavailableStorageRetainMessageForRetry(){
        when(documents.findByBlobContainerAndBlobNameAndDeletedAtIsNull(anyString(),anyString())).thenReturn(Optional.empty());
        assertThat(service.consume(event("No threats found"))).isFalse();
        when(documents.findByBlobContainerAndBlobNameAndDeletedAtIsNull(anyString(),anyString())).thenReturn(Optional.of(doc));
        when(blobs.version(anyString(),anyString())).thenThrow(new IllegalStateException("private provider diagnostics"));
        assertThat(service.consume(event("No threats found"))).isFalse();verifyNoInteractions(database);
    }
    @Test void duplicateCleanDoesNotResetExtractionAndLaterMaliciousBlocks(){
        service.consume(event("No threats found"));doc.setProcessingStatus(ProcessingStatus.EXTRACTED);
        service.consume(event("No threats found"));assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.EXTRACTED);
        service.consume(event("Malicious"));assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.INFECTED);
    }
    @Test void deletedOrChangedVersionUnderLockIsNotCleared(){
        doAnswer(i->{doc.setDeletedAt(Instant.now());return ((java.util.function.Supplier<?>)i.getArgument(0)).get();}).when(database).write(any());
        service.consume(event("No threats found"));assertThat(doc.getScanStatus()).isEqualTo(ScanStatus.PENDING);verify(documents,never()).save(any());
    }
    @Test void malformedMessagesAndRawExtractionEventsAreDiscarded(){
        assertThat(service.consume("bad JSON")).isTrue();assertThat(service.consume("[]")).isTrue();
        assertThat(service.consume(event("No threats found").replace("/quarantine/synthetic.png","/quarantine/extractions/raw.txt"))).isTrue();
        verifyNoInteractions(documents,blobs,database);
    }
}
