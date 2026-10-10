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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final ClaimDocumentRepository documentRepository;
    private final ExtractionResultRepository extractionRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final BlobStorageService blobStorageService;
    private final DocumentIntelligenceClientWrapper ocrClient;
    private final StructuringService structuringService;
    private final AppProperties appProperties;

    public ExtractionService(ClaimDocumentRepository documentRepository,
                             ExtractionResultRepository extractionRepository,
                             ExtractedFieldRepository fieldRepository,
                             BlobStorageService blobStorageService,
                             DocumentIntelligenceClientWrapper ocrClient,
                             StructuringService structuringService,
                             AppProperties appProperties) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.fieldRepository = fieldRepository;
        this.blobStorageService = blobStorageService;
        this.ocrClient = ocrClient;
        this.structuringService = structuringService;
        this.appProperties = appProperties;
    }

    public ExtractionResponse extract(UUID documentId) {
        ClaimDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        if (doc.getScanStatus() != ScanStatus.CLEAN) {
            throw new UnsafeFileException("Document has not passed malware scanning");
        }

        ExtractionResult result = new ExtractionResult();
        result.setDocument(doc);
        result.setStatus(ExtractionStatus.PENDING);
        result.setExtractor("DOCUMENT_INTELLIGENCE");
        result.setExtractorModel(appProperties.docintel().model());
        result.setPromptVersion(appProperties.ai().promptVersion());
        result.setSchemaVersion("v1");

        doc.setProcessingStatus(ProcessingStatus.EXTRACTING);
        documentRepository.save(doc);

        try (InputStream in = blobStorageService.retrieve(doc.getBlobContainer(), doc.getBlobName())) {
            DocumentIntelligenceClientWrapper.OcrResult ocr = ocrClient.analyse(in, doc.getContentType());

            if (!ocr.success()) {
                result.setStatus(ExtractionStatus.FAILED);
                result.setErrorCode(ocr.errorCode());
                result.setErrorMessage(ocr.errorMessage());
                ExtractionResult saved = extractionRepository.save(result);
                doc.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(doc);
                return toResponse(saved);
            }

            doc.setPageCount(ocr.pageCount());

            ClaimFactsExtraction facts = structuringService.structure(documentId, ocr.rawText());
            result.setInjectionSuspected(Boolean.TRUE.equals(facts.getInjectionSuspected()));
            result.setStructuredJson(null);

            List<ExtractedField> fields = new ArrayList<>();
            addIfPresent(fields, result, doc, "policyNumber", facts.getPolicyNumber());
            addIfPresent(fields, result, doc, "policyholderName", facts.getPolicyholderName());
            addIfPresent(fields, result, doc, "driverName", facts.getDriverName());
            addIfPresent(fields, result, doc, "vehicleRegistration", facts.getVehicleRegistration());
            addIfPresent(fields, result, doc, "incidentDate", facts.getIncidentDate());
            addIfPresent(fields, result, doc, "incidentLocation", facts.getIncidentLocation());
            addIfPresent(fields, result, doc, "policeReportNumber", facts.getPoliceReportNumber());
            addIfPresent(fields, result, doc, "garageName", facts.getGarageName());

            result.setOverallConfidence(averageConfidence(fields));
            result.setStatus(fields.isEmpty() ? ExtractionStatus.PARTIAL : ExtractionStatus.SUCCEEDED);

            ExtractionResult saved = extractionRepository.save(result);
            fieldRepository.saveAll(fields);

            doc.setProcessingStatus(ProcessingStatus.EXTRACTED);
            documentRepository.save(doc);

            return toResponse(saved, fields);
        } catch (ExtractionFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Extraction failed for document {}", documentId, e);
            result.setStatus(ExtractionStatus.FAILED);
            result.setErrorCode("EXTRACTION_FAILED");
            result.setErrorMessage(e.getMessage());
            ExtractionResult saved = extractionRepository.save(result);
            doc.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(doc);
            return toResponse(saved);
        }
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