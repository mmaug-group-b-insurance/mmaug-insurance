package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.common.util.HashUtil;
import com.mmaug.claimsagent.document.dto.DocumentResponse;
import com.mmaug.claimsagent.document.dto.DocumentUploadResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final FileValidator fileValidator;
    private final BlobStorageService blobStorageService;
    private final MalwareScanner malwareScanner;

    public DocumentService(ClaimRepository claimRepository,
                           ClaimDocumentRepository documentRepository,
                           FileValidator fileValidator,
                           BlobStorageService blobStorageService,
                           MalwareScanner malwareScanner) {
        this.claimRepository = claimRepository;
        this.documentRepository = documentRepository;
        this.fileValidator = fileValidator;
        this.blobStorageService = blobStorageService;
        this.malwareScanner = malwareScanner;
    }

    public DocumentUploadResponse upload(UUID claimId, MultipartFile file,
                                         DocumentType documentType, CurrentUser user) {
        Claim claim = claimRepository.findForDocumentUpload(claimId)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + claimId));

        assertCanModify(claim, user);

        long count = documentRepository.countByClaimIdAndDeletedAtIsNull(claimId);
        String validatedContentType = fileValidator.validate(file, count);

        String sha256 = computeSha256(file);
        if (documentRepository.existsByClaimIdAndSha256AndDeletedAtIsNull(claimId, sha256)) {
            throw new com.mmaug.claimsagent.common.exception.DuplicateDocumentException();
        }
        String blobName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        String container = "quarantine";

        try (InputStream in = file.getInputStream()) {
            blobStorageService.store(container, blobName, in, file.getSize(), validatedContentType);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        ClaimDocument doc = new ClaimDocument();
        doc.setClaim(claim);
        doc.setOriginalFilename(file.getOriginalFilename());
        doc.setBlobContainer(container);
        doc.setBlobName(blobName);
        doc.setContentType(validatedContentType);
        doc.setSizeBytes(file.getSize());
        doc.setSha256(sha256);
        doc.setStorageEtag(blobStorageService.version(container, blobName));
        doc.setDocumentType(documentType != null ? documentType : DocumentType.OTHER);
        doc.setScanStatus(ScanStatus.PENDING);
        doc.setProcessingStatus(ProcessingStatus.QUARANTINED);
        doc.setUploadedByUserId(UUID.fromString(user.userId()));
        doc.setUploadedAt(Instant.now());

        doc = documentRepository.save(doc);

        // Run the malware scanner synchronously in this phase.
        ScanStatus scanStatus = malwareScanner.scan(container, blobName);
        doc.setScanStatus(scanStatus);
        if (scanStatus == ScanStatus.CLEAN) {
            doc.setProcessingStatus(ProcessingStatus.UPLOADED);
        } else if (scanStatus == ScanStatus.INFECTED) {
            doc.setProcessingStatus(ProcessingStatus.FAILED);
        } else {
            doc.setProcessingStatus(ProcessingStatus.QUARANTINED);
        }
        doc = documentRepository.save(doc);

        return DocumentUploadResponse.builder()
                .id(doc.getId())
                .claimId(claim.getId())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .scanStatus(doc.getScanStatus().name())
                .processingStatus(doc.getProcessingStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID claimId, CurrentUser user) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + claimId));
        assertCanRead(claim, user);
        return documentRepository.findAllByClaimIdAndDeletedAtIsNull(claimId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClaimDocument getDocument(UUID documentId, CurrentUser user) {
        ClaimDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        if (doc.getDeletedAt() != null) {
            throw new NotFoundException("Document not found: " + documentId);
        }
        assertCanRead(doc.getClaim(), user);
        return doc;
    }

    public InputStream download(UUID documentId, CurrentUser user) {
        ClaimDocument doc = getDocument(documentId, user);
        if (doc.getScanStatus() != ScanStatus.CLEAN) {
            throw new com.mmaug.claimsagent.common.exception.UnsafeFileException("Document is quarantined or has not passed malware scanning");
        }
        return doc.getStorageEtag() == null
                ? blobStorageService.retrieve(doc.getBlobContainer(), doc.getBlobName())
                : blobStorageService.retrieveVersion(doc.getBlobContainer(), doc.getBlobName(), doc.getStorageEtag());
    }

    public void softDelete(UUID documentId, CurrentUser user) {
        ClaimDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        boolean isAdmin = user.hasRole(Role.ADMIN);
        boolean isOwner = user.userId().equals(doc.getUploadedByUserId().toString());
        boolean isDraft = doc.getClaim().getStatus() == com.mmaug.claimsagent.claim.ClaimStatus.DRAFT;

        if (!isAdmin && !(isOwner && isDraft)) {
            throw new ForbiddenException("Not permitted to delete this document");
        }

        // Serialize deletion with snapshot capture/publication, as upload and review already do.
        claimRepository.findForDocumentUpload(doc.getClaim().getId()).orElseThrow(() -> new NotFoundException("Claim not found"));
        doc.setDeletedAt(Instant.now());
        documentRepository.save(doc);
    }

    private void assertCanRead(Claim claim, CurrentUser user) {
        boolean officer = user.hasRole(Role.CLAIMS_OFFICER);
        boolean auditor = user.hasRole(Role.AUDITOR);
        boolean admin = user.hasRole(Role.ADMIN);
        boolean owner = user.userId().equals(claim.getCreatedByUserId().toString());
        if (!officer && !auditor && !admin && !owner) {
            throw new ForbiddenException("Not permitted to access this claim's documents");
        }
    }

    private void assertCanModify(Claim claim, CurrentUser user) {
        boolean officer = user.hasRole(Role.CLAIMS_OFFICER);
        boolean owner = user.userId().equals(claim.getCreatedByUserId().toString());
        if (!officer && !owner) {
            throw new ForbiddenException("Not permitted to modify this claim's documents");
        }
    }

    private DocumentResponse toResponse(ClaimDocument doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .claimId(doc.getClaim().getId())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .documentType(doc.getDocumentType())
                .detectedType(doc.getDetectedType())
                .scanStatus(doc.getScanStatus())
                .processingStatus(doc.getProcessingStatus())
                .pageCount(doc.getPageCount())
                .sha256(doc.getSha256())
                .uploadedBy(doc.getUploadedByUserId())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }

    private String computeSha256(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to compute file hash", e);
        }
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        String base = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.length() > 100) {
            base = base.substring(base.length() - 100);
        }
        return base;
    }
}
