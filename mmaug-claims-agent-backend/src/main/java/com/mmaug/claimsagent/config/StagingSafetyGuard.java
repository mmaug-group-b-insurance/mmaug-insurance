package com.mmaug.claimsagent.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.net.URI;
import java.util.Locale;
import java.util.List;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;

/** Fail closed when a shared environment accidentally enables development settings. */
@Component
@Profile({"staging", "prod", "production"})
public class StagingSafetyGuard implements SmartInitializingSingleton {
    private final Environment environment;
    public StagingSafetyGuard(Environment environment) { this.environment = environment; }

    @Override public void afterSingletonsInstantiated() {
        if (Arrays.stream(environment.getActiveProfiles()).map(p -> p.toLowerCase(Locale.ROOT)).anyMatch(Set.of("local", "dev", "test")::contains)) {
            throw new IllegalStateException("Shared environments cannot activate local/dev/test profiles");
        }
        if (!environment.getProperty("app.security.enabled", Boolean.class, true)) {
            throw new IllegalStateException("Shared environments require authentication");
        }
        String issuer = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "");
        if (!httpsOriginOrPath(issuer)) {
            throw new IllegalStateException("Shared environments require an HTTPS JWT issuer");
        }
        String locations = environment.getProperty("spring.flyway.locations", "");
        if (locations.contains("db/seed")) {
            throw new IllegalStateException("Shared environments cannot automatically import development seed data");
        }
        if (environment.getProperty("app.security.audience", "").isBlank()) {
            throw new IllegalStateException("Shared environments require an explicit API audience");
        }
        List<String> origins = Binder.get(environment)
                .bind("app.security.cors.allowed-origins", Bindable.listOf(String.class)).orElse(List.of());
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> !httpsOrigin(origin))) {
            throw new IllegalStateException("Shared environments require explicit HTTPS CORS origins without wildcards or localhost");
        }
        if (!"azure".equals(environment.getProperty("app.docintel.provider"))
                || !"azure".equals(environment.getProperty("app.ai.summary-provider"))) {
            throw new IllegalStateException("Shared environments require Azure OCR and summary providers");
        }
        if (!"azure".equals(environment.getProperty("app.storage.provider"))
                || !"defender-queue".equals(environment.getProperty("app.scanner.provider"))) {
            throw new IllegalStateException("Shared environments require private Azure storage and Defender queue scanning");
        }
        if (!"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))) {
            throw new IllegalStateException("Shared environments require schema validation; use Flyway for changes");
        }
        if (environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true)
                || environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true)) {
            throw new IllegalStateException("Shared environments must disable public API documentation");
        }
    }

    private boolean httpsOriginOrPath(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null && !host.contains("*")
                    && !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1")
                    && !host.equals("[::1]") && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException e) { return false; }
    }

    private boolean httpsOrigin(String value) {
        if (!httpsOriginOrPath(value)) return false;
        return URI.create(value).getPath().isEmpty();
    }
}
