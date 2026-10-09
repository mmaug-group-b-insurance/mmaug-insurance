package com.mmaug.claimsagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Storage storage,
        Ai ai,
        Docintel docintel,
        Tools tools,
        Rules rules,
        Retention retention,
        Logging logging
) {
    public record Security(
            String audience,
            Cors cors,
            RateLimit rateLimit
    ) {
        public record Cors(List<String> allowedOrigins) {}
        public record RateLimit(Integer uploadPerMinute, Integer analysePerMinute) {}
    }

    public record Storage(
            String accountName,
            String quarantineContainer,
            String cleanContainer,
            Integer maxFileSizeMb,
            List<String> allowedTypes,
            Integer maxFilesPerClaim,
            String basePath
    ) {}

    public record Ai(
            String endpoint,
            String deployment,
            Double temperature,
            Integer maxTokens,
            Integer timeoutSeconds,
            String promptVersion,
            String toolSchemaVersion,
            Integer maxToolIterations
    ) {}

    public record Docintel(String endpoint, String model) {}

    public record Tools(String baseUrl, Integer timeoutMs, Integer retries) {}

    public record Rules(String path) {}

    public record Retention(Integer documentDays, Integer auditDays, Integer summaryDays) {}

    public record Logging(Boolean maskPii) {}
}