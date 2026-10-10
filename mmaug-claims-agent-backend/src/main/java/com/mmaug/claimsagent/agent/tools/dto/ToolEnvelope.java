package com.mmaug.claimsagent.agent.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Uniform tool response envelope")
public class ToolEnvelope<T> {

    @Schema(description = "Result status", example = "OK")
    private String status;

    private T data;

    private String errorCode;
    private String errorMessage;
    private String sourceRef;
    private Instant retrievedAt;
    private String toolSchemaVersion;

    public static <T> ToolEnvelope<T> ok(T data, String sourceRef, String version) {
        return ToolEnvelope.<T>builder()
                .status("OK")
                .data(data)
                .sourceRef(sourceRef)
                .retrievedAt(Instant.now())
                .toolSchemaVersion(version)
                .build();
    }

    public static <T> ToolEnvelope<T> notFound(String errorCode, String message, String sourceRef, String version) {
        return ToolEnvelope.<T>builder()
                .status("NOT_FOUND")
                .errorCode(errorCode)
                .errorMessage(message)
                .sourceRef(sourceRef)
                .retrievedAt(Instant.now())
                .toolSchemaVersion(version)
                .build();
    }

    public static <T> ToolEnvelope<T> error(String errorCode, String message, String sourceRef, String version) {
        return ToolEnvelope.<T>builder()
                .status("ERROR")
                .errorCode(errorCode)
                .errorMessage(message)
                .sourceRef(sourceRef)
                .retrievedAt(Instant.now())
                .toolSchemaVersion(version)
                .build();
    }
}