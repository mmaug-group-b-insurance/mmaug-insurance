package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.document.BlobStorageService;
import com.mmaug.claimsagent.document.ClaimDocument;
import com.mmaug.claimsagent.document.ProcessingStatus;
import com.mmaug.claimsagent.document.ScanStatus;
import com.mmaug.claimsagent.document.ClaimDocumentRepository;
import com.mmaug.claimsagent.extraction.dto.ClaimFactsExtraction;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import com.mmaug.claimsagent.extraction.dto.FieldValue;
import com.mmaug.claimsagent.common.exception.ExtractionFailedException;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.common.exception.UnsafeFileException;
import com.mmaug.claimsagent.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final ClaimDocumentRepository documentRepository;
    private final ExtractionResultRepository extractionRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final BlobStorageService blobStorageService;
    private final DocumentIntelligenceClientWrapper ocrClient;
    private final StructuringService structuringService;
    private final AppProperties appProperties;
    private final com.mmaug.claimsagent.common.DatabaseWork database;

    public ExtractionService(ClaimDocumentRepository documentRepository,
                             ExtractionResultRepository extractionRepository,
                             ExtractedFieldRepository fieldRepository,
                             BlobStorageService blobStorageService,
                             DocumentIntelligenceClientWrapper ocrClient,
                             StructuringService structuringService,
                             AppProperties appProperties, com.mmaug.claimsagent.common.DatabaseWork database) {
        this.database = database;
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.fieldRepository = fieldRepository;
        this.blobStorageService = blobStorageService;
        this.ocrClient = ocrClient;
        this.structuringService = structuringService;
        this.appProperties = appProperties;
    }

    private record OcrInput(UUID id, String container, String blob, String type, String sha256, UUID token, String etag) {}

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NEVER)
    public ExtractionResponse extract(UUID documentId) {
        OcrInput input = database.write(() -> {
            ClaimDocument doc = documentRepository.findForExtraction(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
            if(doc.getDeletedAt()!=null || doc.getScanStatus()!=ScanStatus.CLEAN)throw new UnsafeFileException("Document has not passed malware scanning");
            if(doc.getProcessingStatus()==ProcessingStatus.EXTRACTING && doc.getExtractionExpiresAt()!=null && doc.getExtractionExpiresAt().isAfter(java.time.Instant.now()))
                throw new com.mmaug.claimsagent.common.exception.BusinessValidationException("Document extraction is already active");
            if(doc.getExtractionToken()!=null){
                extractionRepository.findById(doc.getExtractionToken()).filter(old->old.getStatus()==ExtractionStatus.PENDING && old.getDocument().getId().equals(doc.getId())).ifPresent(old->{
                    old.setStatus(ExtractionStatus.FAILED);old.setErrorCode("EXTRACTION_INTERRUPTED");old.setErrorMessage("Extraction reservation expired; explicit retry started");extractionRepository.save(old);
                });
            }
            var pending=newResult(doc);pending.setStatus(ExtractionStatus.PENDING);
            pending=extractionRepository.saveAndFlush(pending);
            doc.setExtractionToken(pending.getId());doc.setExtractionExpiresAt(java.time.Instant.now().plusSeconds(300));
            doc.setProcessingStatus(ProcessingStatus.EXTRACTING);
            documentRepository.save(doc);
            return new OcrInput(doc.getId(), doc.getBlobContainer(), doc.getBlobName(), doc.getContentType(), doc.getSha256(), doc.getExtractionToken(), doc.getStorageEtag());
        });
        DocumentIntelligenceClientWrapper.OcrResult ocr;
        ClaimFactsExtraction facts = null;
        String rawName = null;
        try (InputStream in = input.etag()==null ? blobStorageService.retrieve(input.container(), input.blob()) : blobStorageService.retrieveVersion(input.container(), input.blob(), input.etag())) {
            // External evidence must match both the scanned storage version and the upload digest.
            if(input.etag()!=null){
                byte[] bytes=in.readNBytes(15*1024*1024+1);
                if(bytes.length>15*1024*1024 || !java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)).equals(input.sha256()))throw new UnsafeFileException("Stored evidence differs from upload");
                ocr=ocrClient.analyse(new ByteArrayInputStream(bytes),input.type());
            }else ocr = ocrClient.analyse(in, input.type());
            if (ocr.success()) {
                if (ocr.rawText() == null || ocr.pageCount() < 1) throw new IllegalStateException("OCR returned no valid text/page metadata");
                facts = structuringService.structure(documentId, ocr.rawText());
                byte[] raw = ocr.rawText().getBytes(StandardCharsets.UTF_8);
                rawName = "extractions/" + documentId + "/" + UUID.randomUUID() + ".txt";
                blobStorageService.store(input.container(), rawName, new ByteArrayInputStream(raw), raw.length, "text/plain; charset=utf-8");
            }
        } catch (Exception e) {
            log.warn("Extraction provider failed for document {} ({})", documentId, e.getClass().getSimpleName());
            ocr = DocumentIntelligenceClientWrapper.OcrResult.failed("EXTRACTION_FAILED", "Document extraction failed; retry or replace the document");
        }
        final var completedOcr = ocr;
        final var completedFacts = facts;
        final var completedRawName = rawName;
        return database.write(() -> complete(input, completedOcr, completedFacts, completedRawName));
    }

    private ClaimDocument safeDocument(UUID id) {
        var doc = documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
        if (doc.getDeletedAt() != null || doc.getScanStatus() != ScanStatus.CLEAN) throw new UnsafeFileException("Document has not passed malware scanning");
        return doc;
    }

    private ExtractionResult newResult(ClaimDocument doc){
        var result=new ExtractionResult();result.setDocument(doc);result.setExtractor("DOCUMENT_INTELLIGENCE");result.setExtractorModel(appProperties.docintel().model());
        result.setPromptVersion(appProperties.ai().promptVersion());result.setSchemaVersion("v1");return result;
    }

    private ExtractionResponse complete(OcrInput input, DocumentIntelligenceClientWrapper.OcrResult ocr, ClaimFactsExtraction facts, String rawName) {
        ClaimDocument doc = safeDocument(input.id());
        if (!java.util.Objects.equals(input.token(),doc.getExtractionToken()) || doc.getExtractionExpiresAt()==null || !doc.getExtractionExpiresAt().isAfter(java.time.Instant.now()))
            throw new UnsafeFileException("Extraction reservation is no longer active");
        doc.setExtractionToken(null);doc.setExtractionExpiresAt(null);
        if (!java.util.Objects.equals(input.sha256(), doc.getSha256())) throw new UnsafeFileException("Document changed during extraction");
        ExtractionResult result = extractionRepository.findById(input.token()).orElseThrow(() -> new UnsafeFileException("Extraction reservation not found"));
        if(!result.getDocument().getId().equals(doc.getId()) || result.getStatus()!=ExtractionStatus.PENDING)throw new UnsafeFileException("Extraction reservation is no longer pending");
        if (!ocr.success()) {
            result.setStatus(ExtractionStatus.FAILED);result.setErrorCode(ocr.errorCode());result.setErrorMessage(ocr.errorMessage());
            var saved = extractionRepository.save(result);doc.setProcessingStatus(ProcessingStatus.FAILED);documentRepository.save(doc);
            return toResponse(saved);
        }
        doc.setPageCount(ocr.pageCount());result.setInjectionSuspected(Boolean.TRUE.equals(facts.getInjectionSuspected()));
        result.setStructuredJson(JsonMapper.builder().build().writeValueAsString(facts));result.setRawTextBlobName(rawName);
        List<ExtractedField> fields = new ArrayList<>();
        addIfPresent(fields,result,doc,"policyNumber",facts.getPolicyNumber());
        addIfPresent(fields,result,doc,"policyholderName",facts.getPolicyholderName());
        addIfPresent(fields,result,doc,"driverName",facts.getDriverName());
        addIfPresent(fields,result,doc,"vehicleRegistration",facts.getVehicleRegistration());
        addIfPresent(fields,result,doc,"incidentDate",facts.getIncidentDate());
        addIfPresent(fields,result,doc,"incidentLocation",facts.getIncidentLocation());
        addIfPresent(fields,result,doc,"policeReportNumber",facts.getPoliceReportNumber());
        addIfPresent(fields,result,doc,"garageName",facts.getGarageName());
        addIfPresent(fields,result,doc,"damageDescription",facts.getDamageDescription());
        addIfPresent(fields,result,doc,"estimatedRepairCost",facts.getEstimatedRepairCost());
        addIfPresent(fields,result,doc,"currency",facts.getCurrency());
        result.setOverallConfidence(averageConfidence(fields));result.setStatus(fields.isEmpty()?ExtractionStatus.PARTIAL:ExtractionStatus.SUCCEEDED);
        var saved=extractionRepository.save(result);fieldRepository.saveAll(fields);doc.setProcessingStatus(ProcessingStatus.EXTRACTED);documentRepository.save(doc);
        return toResponse(saved,fields);
    }

    @Transactional(readOnly = true)
    public ExtractionResponse getForDocument(UUID documentId) {
        ExtractionResult result = extractionRepository
                .findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new NotFoundException("No extraction found for document: " + documentId));
        List<ExtractedField> fields = fieldRepository.findAllByExtractionId(result.getId());
        return toResponse(result, fields);
    }

    private void addIfPresent(List<ExtractedField> fields, ExtractionResult parent, ClaimDocument doc,
                              String fieldName, FieldValue value) {
        if (value == null) return;
        ExtractedField field = new ExtractedField();
        field.setExtraction(parent);
        field.setDocument(doc);
        field.setFieldName(fieldName);
        field.setFieldValue(value.getValue());
        field.setNormalisedValue(value.getValue());
        field.setConfidence(value.getConfidence());
        field.setPageNumber(value.getPage());
        field.setSnippet(value.getSnippet());
        fields.add(field);
    }

    private BigDecimal averageConfidence(List<ExtractedField> fields) {
        if (fields.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (ExtractedField f : fields) {
            if (f.getConfidence() != null) {
                sum = sum.add(f.getConfidence());
                count++;
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(count), 3, java.math.RoundingMode.HALF_UP);
    }

    private ExtractionResponse toResponse(ExtractionResult result) {
        return toResponse(result, fieldRepository.findAllByExtractionId(result.getId()));
    }

    private ExtractionResponse toResponse(ExtractionResult result, List<ExtractedField> fields) {
        List<ExtractionResponse.ExtractedFieldDto> dtos = new ArrayList<>();
        for (ExtractedField f : fields) {
            dtos.add(ExtractionResponse.ExtractedFieldDto.builder()
                    .fieldName(f.getFieldName())
                    .value(f.getFieldValue())
                    .normalisedValue(f.getNormalisedValue())
                    .confidence(f.getConfidence())
                    .page(f.getPageNumber())
                    .snippet(f.getSnippet())
                    .build());
        }
        return ExtractionResponse.builder()
                .extractionId(result.getId())
                .documentId(result.getDocument().getId())
                .status(result.getStatus())
                .extractorModel(result.getExtractorModel())
                .llmModel(result.getLlmModel())
                .promptVersion(result.getPromptVersion())
                .schemaVersion(result.getSchemaVersion())
                .overallConfidence(result.getOverallConfidence())
                .injectionSuspected(result.getInjectionSuspected())
                .fields(dtos)
                .build();
    }
}
