package com.mmaug.claimsagent.extraction;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AzureDocumentIntelligenceClientTest {
 @SuppressWarnings("unchecked")
 HttpResponse<String> response(int code,String location,String body) {
  HttpResponse<String> r=mock(HttpResponse.class);
  when(r.statusCode()).thenReturn(code);
  when(r.headers()).thenReturn(HttpHeaders.of(location==null?Map.of():Map.of("Operation-Location",List.of(location)),(a,b)->true));
  when(r.body()).thenReturn(body); return r;
 }
 @Test void extractsPagesIncludingBlankPage() throws Exception {
  HttpClient h=mock(HttpClient.class);
  doReturn(response(202,"https://ocr.example/documentintelligence/documentModels/prebuilt-layout/analyzeResults/1", ""),response(200,null,"{\"status\":\"succeeded\",\"analyzeResult\":{\"pages\":[{\"pageNumber\":1,\"lines\":[]},{\"pageNumber\":2,\"lines\":[{\"content\":\"Policy number: SYN-42\"}]}]}}")).when(h).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
  var r=new AzureDocumentIntelligenceClient("https://ocr.example","key",h).analyse(new ByteArrayInputStream(new byte[]{1}),"application/pdf");
  assertTrue(r.success()); assertEquals(2,r.pageCount()); assertEquals("\fPolicy number: SYN-42\n",r.rawText());
 }
 @Test void rejectsCredentialForwarding() throws Exception {
  HttpClient h=mock(HttpClient.class);
  doReturn(response(202,"https://other.example/steal", "")).when(h).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
  var r=new AzureDocumentIntelligenceClient("https://ocr.example","key",h).analyse(new ByteArrayInputStream(new byte[]{1}),"image/png");
  assertEquals("OCR_INVALID_OPERATION",r.errorCode()); verify(h,times(1)).send(any(),any());
 }
 @Test void deniedIsSanitized() throws Exception {
  HttpClient h=mock(HttpClient.class);
  doReturn(response(403,null,"secret diagnostic")).when(h).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
  var r=new AzureDocumentIntelligenceClient("https://ocr.example","key",h).analyse(new ByteArrayInputStream(new byte[]{1}),"image/png");
  assertEquals("OCR_ACCESS_DENIED",r.errorCode()); assertFalse(r.errorMessage().contains("secret"));
 }
 @Test void rejectsHttpEndpoint() { assertThrows(IllegalArgumentException.class,()->new AzureDocumentIntelligenceClient("http://ocr.example","key",mock(HttpClient.class))); }
}
