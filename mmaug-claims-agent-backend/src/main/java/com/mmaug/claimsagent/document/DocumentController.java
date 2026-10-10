package com.mmaug.claimsagent.document;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.document.dto.DocumentResponse;
import com.mmaug.claimsagent.document.dto.DocumentUploadResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Tag(name = "Documents", description = "Upload, list, download and delete claim documents")
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
            summary = "Upload claim document",
            description = """
                    Uploads a supported document to an existing claim.
                    Allowed types: PDF, JPG, JPEG, PNG, TIFF. Maximum 15 MB per file.
                    Maximum 20 files per claim. Documents are placed in quarantine first,
                    then scanned before processing.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "413", description = "File too large",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Unsafe file",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/claims/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(
            @PathVariable UUID claimId,
            @Parameter(description = "File to upload", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Declared document type")
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            CurrentUser user) {
        DocumentUploadResponse response = documentService.upload(claimId, file, documentType, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List claim documents",
            description = "Lists all active documents associated with a claim.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents listed"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Claim not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/claims/{claimId}/documents")
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable UUID claimId, CurrentUser user) {
        return ResponseEntity.ok(documentService.list(claimId, user));
    }

    @Operation(summary = "Download document",
            description = "Streams the original file to the caller. Access is role- and owner-restricted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File streamed"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Document not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID documentId, CurrentUser user) {
        ClaimDocument doc = documentService.getDocument(documentId, user);
        InputStream in = documentService.download(documentId, user);
        InputStreamResource resource = new InputStreamResource(in);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(resource);
    }

    @Operation(summary = "Delete document",
            description = "Soft-deletes a document. Owners may delete documents on draft claims; admins may delete any.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document deleted"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Document not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId, CurrentUser user) {
        documentService.softDelete(documentId, user);
        return ResponseEntity.noContent().build();
    }
}