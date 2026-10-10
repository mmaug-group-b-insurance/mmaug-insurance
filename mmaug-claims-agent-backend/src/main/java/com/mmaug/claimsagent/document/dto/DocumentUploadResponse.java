package com.mmaug.claimsagent.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after uploading a document")
public class DocumentUploadResponse {

    private UUID id;
    private UUID claimId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String scanStatus;
    private String processingStatus;
}