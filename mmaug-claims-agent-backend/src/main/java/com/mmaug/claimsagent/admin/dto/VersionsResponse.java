package com.mmaug.claimsagent.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Application and configuration version metadata")
public class VersionsResponse {

    @Schema(description = "Application version", example = "1.0.0-SNAPSHOT")
    private String appVersion;

    @Schema(description = "Git commit SHA")
    private String gitCommit;

    @Schema(description = "Prompt version", example = "v1")
    private String promptVersion;

    @Schema(description = "Tool schema version", example = "v1")
    private String toolSchemaVersion;

    @Schema(description = "Business rules version", example = "1.0.0")
    private String ruleSetVersion;

    @Schema(description = "Business rules SHA-256 checksum")
    private String ruleSetSha256;
}