package com.mmaug.claimsagent.document.dto;

import com.mmaug.claimsagent.document.DocumentType;
import com.mmaug.claimsagent.document.ProcessingStatus;
import com.mmaug.claimsagent.document.ScanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Claim document metadata")
public class DocumentResponse {

    private UUID id;
    private UUID claimId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private DocumentType documentType;
    private DocumentType detectedType;
    private ScanStatus scanStatus;
    private ProcessingStatus processingStatus;
    private Integer pageCount;
    private String sha256;
    private UUID uploadedBy;
    private Instant uploadedAt;
}