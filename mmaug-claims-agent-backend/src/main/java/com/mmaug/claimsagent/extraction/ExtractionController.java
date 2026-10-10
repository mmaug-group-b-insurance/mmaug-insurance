package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.extraction.dto.ExtractionResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Extraction", description = "Document extraction and structured field retrieval")
@RestController
@RequestMapping("/api/v1/documents")
public class ExtractionController {

    private final ExtractionService extractionService;

    public ExtractionController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @Operation(
            summary = "Get extraction result for a document",
            description = "Returns the latest extraction and structured fields for the given document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extraction retrieved"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Document or extraction not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{documentId}/extraction")
    public ResponseEntity<ExtractionResponse> getExtraction(@PathVariable UUID documentId, CurrentUser user) {
        return ResponseEntity.ok(extractionService.getForDocument(documentId));
    }

    @Operation(
            summary = "Trigger extraction for a document",
            description = """
                    Runs OCR and structuring on a document that has passed malware scanning.
                    The default implementation is deterministic and does not require external AI credentials.
                    Production deployments replace the OCR client with Azure Document Intelligence.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Extraction started"),
            @ApiResponse(responseCode = "403", description = "Not permitted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Document not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Document not clean or extraction failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{documentId}/extraction")
    public ResponseEntity<ExtractionResponse> extract(@PathVariable UUID documentId, CurrentUser user) {
        return ResponseEntity.accepted().body(extractionService.extract(documentId));
    }
}