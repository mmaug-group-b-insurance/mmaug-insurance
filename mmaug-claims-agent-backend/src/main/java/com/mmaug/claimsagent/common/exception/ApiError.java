package com.mmaug.claimsagent.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard API error response")
public record ApiError(
        @Schema(description = "Error timestamp", example = "2026-01-01T12:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Machine-readable error code", example = "VALIDATION_FAILED")
        String code,

        @Schema(description = "Human-readable error message", example = "Validation failed")
        String message,

        @Schema(description = "Request path", example = "/api/v1/claims")
        String path,

        @Schema(description = "Correlation ID for tracing", example = "a1b2c3d4")
        String correlationId,

        @Schema(description = "Field-level validation errors")
        List<FieldError> fieldErrors
) {
    public record FieldError(
            @Schema(description = "Field name", example = "policyNumber")
            String field,
            @Schema(description = "Error message", example = "must match POL-MT-######")
            String message
    ) {}
}