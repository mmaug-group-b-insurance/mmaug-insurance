package com.mmaug.claimsagent.admin;

import com.mmaug.claimsagent.admin.dto.VersionsResponse;
import com.mmaug.claimsagent.common.exception.ApiError;
import com.mmaug.claimsagent.common.util.HashUtil;
import com.mmaug.claimsagent.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "System information and version metadata")
@RestController
@RequestMapping("/api/v1/admin/versions")
public class VersionsController {

    private final AppProperties appProperties;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @Value("${app.git-commit:unknown}")
    private String gitCommit;

    public VersionsController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Operation(
            summary = "Get version metadata",
            description = """
                    Returns the application version, git commit, prompt version,
                    tool schema version, business rules version, and the rules checksum.
                    No secrets are exposed.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only admins or officers may view versions",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<VersionsResponse> get() {
        String rulesVersion = "1.0.0";
        String rulesSha = HashUtil.sha256("business-rules-" + rulesVersion);

        return ResponseEntity.ok(VersionsResponse.builder()
                .appVersion(appVersion)
                .gitCommit(gitCommit)
                .promptVersion(appProperties.ai().promptVersion())
                .toolSchemaVersion(appProperties.ai().toolSchemaVersion())
                .ruleSetVersion(rulesVersion)
                .ruleSetSha256(rulesSha)
                .build());
    }
}