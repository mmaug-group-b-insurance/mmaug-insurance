package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.config.AppProperties;
import com.mmaug.claimsagent.document.*;
import com.mmaug.claimsagent.security.*;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadExtractionFlowTest {
 @Test void uploadedClearedEvidenceIsStructuredSavedAndReopenedWithPageProvenance() throws Exception {
  UUID claimId=UUID.randomUUID(), userId=UUID.randomUUID();
  Claim claim=new Claim(); claim.setId(claimId); claim.setCreatedByUserId(userId);
  ClaimRepository claims=mock(ClaimRepository.class);
  when(claims.findForDocumentUpload(claimId)).thenReturn(Optional.of(claim));
  ClaimDocumentRepository documents=mock(ClaimDocumentRepository.class);
  Map<UUID,ClaimDocument> rows=new HashMap<>();
  when(documents.save(any())).thenAnswer(i->{ClaimDocument d=i.getArgument(0); if(d.getId()==null)d.setId(UUID.randomUUID()); rows.put(d.getId(),d); return d;});
  when(documents.findById(any())).thenAnswer(i->Optional.ofNullable(rows.get(i.getArgument(0))));
  BlobStorageService storage=mock(BlobStorageService.class);
  Map<String,byte[]> blobs=new HashMap<>();
  doAnswer(i->{blobs.put(i.getArgument(0)+"/"+i.getArgument(1),((InputStream)i.getArgument(2)).readAllBytes());return null;}).when(storage).store(any(),any(),any(),anyLong(),any());
  when(storage.retrieve(any(),any())).thenAnswer(i->new ByteArrayInputStream(blobs.get(i.getArgument(0)+"/"+i.getArgument(1))));
  MalwareScanner scanner=mock(MalwareScanner.class);
  when(scanner.scan(any(),any())).thenReturn(ScanStatus.CLEAN);
  DocumentService uploads=new DocumentService(claims,documents,new FileValidator(),storage,scanner);
  CurrentUser officer=new CurrentUser(userId.toString(),"synthetic","officer@example.invalid",Set.of(Role.CLAIMS_OFFICER));
  var uploaded=uploads.upload(claimId,new MockMultipartFile("file","synthetic.pdf","application/pdf","%PDF-synthetic fixture".getBytes()),DocumentType.CLAIM_FORM,officer);
  assertThat(uploaded.getScanStatus()).isEqualTo("CLEAN");
  var results=mock(ExtractionResultRepository.class);
  var fields=mock(ExtractedFieldRepository.class);
  List<ExtractionResult> saved=new ArrayList<>(); List<ExtractedField> savedFields=new ArrayList<>();
  Map<UUID,ExtractionResult> pending=new HashMap<>();
  when(results.saveAndFlush(any())).thenAnswer(i->{ExtractionResult r=i.getArgument(0);r.setId(UUID.randomUUID());pending.put(r.getId(),r);return r;});
  when(results.findById(any())).thenAnswer(i->Optional.ofNullable(pending.get(i.getArgument(0))));
  when(results.save(any())).thenAnswer(i->{ExtractionResult r=i.getArgument(0);if(r.getId()==null)r.setId(UUID.randomUUID());saved.add(r);return r;});
  when(fields.saveAll(any())).thenAnswer(i->{Iterable<ExtractedField> batch=i.getArgument(0);batch.forEach(savedFields::add);return savedFields;});
  when(results.findFirstByDocumentIdOrderByCreatedAtDesc(uploaded.getId())).thenAnswer(i->Optional.of(saved.get(saved.size()-1)));
  when(fields.findAllByExtractionId(any())).thenAnswer(i->savedFields);
  var ocr=mock(DocumentIntelligenceClientWrapper.class);
  when(ocr.analyse(any(),eq("application/pdf"))).thenReturn(DocumentIntelligenceClientWrapper.OcrResult.ok("Cover sheet\fPolicy Number: SYN-48271\nVehicle Registration: TEST123",2));
  var properties=new AppProperties(null,null,new AppProperties.Ai(null,null,null,null,null,"v1","v1",null),new AppProperties.Docintel(null,"prebuilt-layout"),null,null,null,null);
  var database=mock(com.mmaug.claimsagent.common.DatabaseWork.class);
  when(database.write(any())).thenAnswer(inv->((java.util.function.Supplier<?>)inv.getArgument(0)).get());
  when(documents.findForExtraction(uploaded.getId())).thenAnswer(inv->Optional.of(rows.get(uploaded.getId())));
  var extraction=new ExtractionService(documents,results,fields,storage,ocr,new StructuringService(),properties,database);
  var controller=new ExtractionController(extraction,uploads);
  var response=controller.extract(uploaded.getId(),officer).getBody();
  assertThat(response.getStatus()).isEqualTo(ExtractionStatus.SUCCEEDED);
  assertThat(response.getFields()).hasSize(2).allSatisfy(f->assertThat(f.getPage()).isEqualTo(2));
  assertThat(controller.getExtraction(uploaded.getId(),officer).getBody().getExtractionId()).isEqualTo(response.getExtractionId());
  assertThat(blobs.get("quarantine/"+saved.get(0).getRawTextBlobName())).isNotEmpty();
  assertThat(rows.get(uploaded.getId()).getProcessingStatus()).isEqualTo(ProcessingStatus.EXTRACTED);
 }
 @Test void deniedDocumentAccessNeverCallsExtraction() {
  var documents=mock(DocumentService.class); var extraction=mock(ExtractionService.class);
  var controller=new ExtractionController(extraction,documents);
  var officer=new CurrentUser(UUID.randomUUID().toString(),"id","test@example.invalid",Set.of(Role.CLAIMS_OFFICER));
  UUID id=UUID.randomUUID();
  when(documents.getDocument(id,officer)).thenThrow(new ForbiddenException("Denied"));
  assertThatThrownBy(()->controller.extract(id,officer)).isInstanceOf(ForbiddenException.class);
  assertThatThrownBy(()->controller.getExtraction(id,officer)).isInstanceOf(ForbiddenException.class);
  verifyNoInteractions(extraction);
 }
 @Test void auditorCannotTriggerPaidOcr() {
  var extraction=mock(ExtractionService.class); var documents=mock(DocumentService.class);
  var user=new CurrentUser(UUID.randomUUID().toString(),"id","test@example.invalid",Set.of(Role.AUDITOR));
  assertThatThrownBy(()->new ExtractionController(extraction,documents).extract(UUID.randomUUID(),user)).isInstanceOf(ForbiddenException.class);
  verifyNoInteractions(extraction,documents);
 }
}
