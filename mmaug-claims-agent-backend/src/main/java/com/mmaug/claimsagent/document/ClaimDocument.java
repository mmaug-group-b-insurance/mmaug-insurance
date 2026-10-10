package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim_document")
@Getter
@Setter
@NoArgsConstructor
public class ClaimDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "blob_container", nullable = false, length = 40)
    private String blobContainer;

    @Column(name = "blob_name", nullable = false, length = 200)
    private String blobName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 25)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_type", length = 25)
    private DocumentType detectedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 15)
    private ScanStatus scanStatus = ScanStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 15)
    private ProcessingStatus processingStatus = ProcessingStatus.UPLOADED;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;
}