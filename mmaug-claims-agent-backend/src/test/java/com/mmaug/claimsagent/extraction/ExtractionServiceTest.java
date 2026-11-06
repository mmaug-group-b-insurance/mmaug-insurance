package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.document.*;
import com.mmaug.claimsagent.config.AppProperties;
import com.mmaug.claimsagent.common.exception.UnsafeFileException;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractionServiceTest {
    private final ClaimDocumentRepository documents = mock(ClaimDocumentRepository.class);
    private final ExtractionResultRepository results = mock(ExtractionResultRepository.class);
    private final ExtractedFieldRepository fields = mock(ExtractedFieldRepository.class);
    private final BlobStorageService blobs = mock(BlobStorageService.class);
    private final DocumentIntelligenceClientWrapper ocr = mock(DocumentIntelligenceClientWrapper.class);
    private final AppProperties properties = new AppProperties(null, null,
            new AppProperties.Ai(null, null, null, null, null, "v1", "v1", null),
            new AppProperties.Docintel(null, "test-ocr"), null, null, null, null);
    private final com.mmaug.claimsagent.common.DatabaseWork database = mock(com.mmaug.claimsagent.common.DatabaseWork.class);
    {
        when(database.write(any())).thenAnswer(inv -> ((java.util.function.Supplier<?>)inv.getArgument(0)).get());
        Map<UUID,ExtractionResult> pending=new HashMap<>();
        when(results.saveAndFlush(any())).thenAnswer(inv->{ExtractionResult result=inv.getArgument(0);result.setId(UUID.randomUUID());pending.put(result.getId(),result);return result;});
        when(results.findById(any())).thenAnswer(inv->Optional.ofNullable(pending.get(inv.getArgument(0))));
    }
    private final ExtractionService service = new ExtractionService(documents, results, fields,
            blobs, ocr, new StructuringService(), properties, database);

    @Test void unclearedEvidenceNeverReachesStorageOrOcr() {
        ClaimDocument doc = document(ScanStatus.PENDING);
        assertThatThrownBy(() -> service.extract(doc.getId())).isInstanceOf(UnsafeFileException.class);
        verifyNoInteractions(blobs, ocr, results, fields);
    }

    @Test void deletedEvidenceNeverReachesStorageOrOcr() {
        ClaimDocument doc = document(ScanStatus.CLEAN); doc.setDeletedAt(java.time.Instant.now());
        assertThatThrownBy(() -> service.extract(doc.getId())).isInstanceOf(UnsafeFileException.class);
        verifyNoInteractions(blobs, ocr, results, fields);
    }

    @Test void providerFailureDoesNotGenerateFactsOrRawText() {
        ClaimDocument doc = document(ScanStatus.CLEAN);
        when(blobs.retrieve("clean", "source")).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(ocr.analyse(any(), any())).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.failed("UNAVAILABLE", "Unavailable"));
        when(results.save(any())).thenAnswer(i -> i.getArgument(0));
        when(fields.findAllByExtractionId(any())).thenReturn(List.of());
        assertThat(service.extract(doc.getId()).getStatus()).isEqualTo(ExtractionStatus.FAILED);
        verify(fields, never()).saveAll(any());
        verify(blobs, never()).store(any(), any(), any(), anyLong(), any());
        assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
    }

    @Test void savesNewSnapshotAndRawEvidenceOnEachExtraction() {
        ClaimDocument doc = document(ScanStatus.CLEAN);
        String text = "Policy Number: POL-MT-SYN001\nRegistration: SYN123";
        when(blobs.retrieve("clean", "source")).thenAnswer(i -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        when(ocr.analyse(any(), any())).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.ok(text, 1));
        List<ExtractionResult> saved = new ArrayList<>();
        when(results.save(any())).thenAnswer(i -> {
            ExtractionResult result = i.getArgument(0); if(result.getId()==null)result.setId(UUID.randomUUID()); saved.add(result); return result;
        });
        var first = service.extract(doc.getId());
        var second = service.extract(doc.getId());
        assertThat(first.getExtractionId()).isNotEqualTo(second.getExtractionId());
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStructuredJson()).contains("POL-MT-SYN001", doc.getId().toString());
        assertThat(saved.get(0).getRawTextBlobName()).isNotEqualTo(saved.get(1).getRawTextBlobName());
        verify(blobs, times(2)).store(eq("clean"), startsWith("extractions/"), any(), anyLong(), eq("text/plain; charset=utf-8"));
        assertThat(first.getFields()).hasSize(2);
    }

    @Test void concurrentExtractionReservationRejectsAnotherPaidCall() {
        var doc=document(ScanStatus.CLEAN);doc.setProcessingStatus(ProcessingStatus.EXTRACTING);doc.setExtractionToken(UUID.randomUUID());doc.setExtractionExpiresAt(java.time.Instant.now().plusSeconds(300));
        assertThatThrownBy(()->service.extract(doc.getId())).isInstanceOf(com.mmaug.claimsagent.common.exception.BusinessValidationException.class);
        verifyNoInteractions(ocr,blobs,fields,results);
    }

    @Test void lateOcrCannotPublishAfterItsDocumentReservationIsReplaced() {
        var doc=document(ScanStatus.CLEAN);
        when(blobs.retrieve(any(),any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(ocr.analyse(any(),any())).thenAnswer(inv->{doc.setExtractionToken(UUID.randomUUID());return DocumentIntelligenceClientWrapper.OcrResult.ok("Policy Number: SYN-TEST",1);});
        assertThatThrownBy(()->service.extract(doc.getId())).isInstanceOf(UnsafeFileException.class);
        verify(results,never()).save(any());verifyNoInteractions(fields);
    }

    private ClaimDocument document(ScanStatus scan) {
        ClaimDocument doc = new ClaimDocument(); doc.setId(UUID.randomUUID()); doc.setScanStatus(scan);
        doc.setBlobContainer("clean"); doc.setBlobName("source"); doc.setContentType("application/pdf");
        when(documents.findForExtraction(doc.getId())).thenReturn(Optional.of(doc));when(documents.findById(doc.getId())).thenReturn(Optional.of(doc)); return doc;
    }
    @Test void externalVersionDigestMismatchNeverReachesPaidOcr() {
        var doc=document(ScanStatus.CLEAN);doc.setStorageEtag("version-one");doc.setSha256("wrong-upload-digest");
        when(blobs.retrieveVersion("clean","source","version-one")).thenReturn(new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8)));
        when(results.save(any())).thenAnswer(i->i.getArgument(0));when(fields.findAllByExtractionId(any())).thenReturn(List.of());
        assertThat(service.extract(doc.getId()).getStatus()).isEqualTo(ExtractionStatus.FAILED);
        verifyNoInteractions(ocr);verify(blobs,never()).retrieve(anyString(),anyString());
    }
}
