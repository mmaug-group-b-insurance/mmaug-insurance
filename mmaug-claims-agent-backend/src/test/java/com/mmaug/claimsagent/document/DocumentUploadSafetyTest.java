package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.claim.*;
import com.mmaug.claimsagent.security.*;
import com.mmaug.claimsagent.common.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentUploadSafetyTest {
 @Test void quarantinedOrInfectedDownloadsNeverReachStorage() {
  var documents=mock(ClaimDocumentRepository.class);var storage=mock(BlobStorageService.class);
  var service=new DocumentService(mock(ClaimRepository.class),documents,new FileValidator(),storage,mock(MalwareScanner.class));
  var claim=new Claim();claim.setCreatedByUserId(UUID.randomUUID());var doc=new ClaimDocument();doc.setId(UUID.randomUUID());doc.setClaim(claim);
  when(documents.findById(doc.getId())).thenReturn(Optional.of(doc));
  var user=new CurrentUser(UUID.randomUUID().toString(),"synthetic","test@example.invalid",Set.of(Role.CLAIMS_OFFICER));
  for(var status:List.of(ScanStatus.PENDING,ScanStatus.INFECTED,ScanStatus.REJECTED)){
   doc.setScanStatus(status);assertThatThrownBy(()->service.download(doc.getId(),user)).isInstanceOf(UnsafeFileException.class);
  }
  verifyNoInteractions(storage);
 }
 @Test void duplicateIsRejectedBeforeStorageOrScanning() {
  var claims=mock(ClaimRepository.class); var documents=mock(ClaimDocumentRepository.class);
  var storage=mock(BlobStorageService.class);var scanner=mock(MalwareScanner.class);
  UUID id=UUID.randomUUID(),userId=UUID.randomUUID(); Claim claim=new Claim();claim.setId(id);claim.setCreatedByUserId(userId);
  when(claims.findForDocumentUpload(id)).thenReturn(Optional.of(claim));
  when(documents.existsByClaimIdAndSha256AndDeletedAtIsNull(eq(id),anyString())).thenReturn(true);
  var service=new DocumentService(claims,documents,new FileValidator(),storage,scanner);
  var user=new CurrentUser(userId.toString(),"synthetic","test@example.invalid",Set.of(Role.CLAIMS_OFFICER));
  assertThatThrownBy(()->service.upload(id,new MockMultipartFile("file","sample.pdf","application/octet-stream","%PDF-synthetic".getBytes()),DocumentType.CLAIM_FORM,user)).isInstanceOf(DuplicateDocumentException.class);
  verifyNoInteractions(storage,scanner);verify(documents,never()).save(any());
  assertThat(new GlobalExceptionHandler().handleDuplicateDocument(new DuplicateDocumentException(),new MockHttpServletRequest()).getStatusCode().value()).isEqualTo(409);
 }
 @Test void verifiedSignatureDeterminesMimeRegardlessOfDeclaredHeader() {
  var validator=new FileValidator();
  assertThat(validator.validate(new MockMultipartFile("file","sample.PNG","application/octet-stream",new byte[]{(byte)0x89,0x50,0x4e,0x47}),0)).isEqualTo("image/png");
  assertThat(validator.validate(new MockMultipartFile("file","sample.jpeg","text/html",new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff}),0)).isEqualTo("image/jpeg");
  assertThat(validator.validate(new MockMultipartFile("file","sample.tiff",null,new byte[]{0x49,0x49,0x2a,0}),0)).isEqualTo("image/tiff");
 }
}
