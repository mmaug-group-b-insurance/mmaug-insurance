package com.mmaug.claimsagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class StagingSafetyGuardTest {
    private MockEnvironment validEnvironment() {
        MockEnvironment env = new MockEnvironment(); env.setActiveProfiles("staging");
        return env.withProperty("app.security.enabled", "true")
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://identity.example.test/tenant/v2.0")
                .withProperty("spring.flyway.locations", "classpath:db/migration")
                .withProperty("app.security.audience", "synthetic-api-client")
                .withProperty("app.security.cors.allowed-origins[0]", "https://claims.example.test")
                .withProperty("app.docintel.provider", "azure")
                .withProperty("app.ai.summary-provider", "azure")
                .withProperty("app.storage.provider", "azure")
                .withProperty("app.scanner.provider", "defender-queue")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("springdoc.api-docs.enabled", "false")
                .withProperty("springdoc.swagger-ui.enabled", "false");
    }
    @Test void acceptsAuthenticatedStagingConfiguration() {
        assertThatCode(() -> new StagingSafetyGuard(validEnvironment()).afterSingletonsInstantiated()).doesNotThrowAnyException();
    }
    @Test void rejectsDevelopmentProfilesInSharedEnvironment() {
        var env = validEnvironment(); env.setActiveProfiles("staging", "local");
        assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("local/dev/test");
    }
    @Test void rejectsAuthenticationBypass() {
        var env = validEnvironment().withProperty("app.security.enabled", "false");
        assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("authentication");
    }
    @Test void rejectsMissingIdentityProvider() {
        var env = validEnvironment().withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "");
        assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("HTTPS JWT issuer");
    }
    @Test void rejectsAutomaticDevelopmentSeeding() {
        var env = validEnvironment().withProperty("spring.flyway.locations", "classpath:db/migration,classpath:db/seed");
        assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("seed data");
    }
    @Test void rejectsMissingAudience() {
        var env=validEnvironment().withProperty("app.security.audience", " ");
        assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("audience");
    }
    @Test void rejectsUnsafeOrigins() {
        for(String origin : new String[]{"*", "https://*.example.test", "http://claims.example.test", "https://localhost", "https://claims.example.test/path", "https://claims.example.test?x=1", "https://user@claims.example.test", "https://claims.example.test/"}) {
            var env=validEnvironment().withProperty("app.security.cors.allowed-origins[0]", origin);
            assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("CORS");
        }
    }
    @Test void rejectsDevelopmentProviders() {
        for(String property : new String[]{"app.docintel.provider", "app.ai.summary-provider"}) {
            var env=validEnvironment().withProperty(property, "stub");
            assertThatThrownBy(() -> new StagingSafetyGuard(env).afterSingletonsInstantiated()).hasMessageContaining("providers");
        }
    }
    @Test void rejectsSchemaMutationAndPublicDocs() {
        assertThatThrownBy(() -> new StagingSafetyGuard(validEnvironment().withProperty("spring.jpa.hibernate.ddl-auto", "update")).afterSingletonsInstantiated()).hasMessageContaining("schema");
        assertThatThrownBy(() -> new StagingSafetyGuard(validEnvironment().withProperty("springdoc.api-docs.enabled", "true")).afterSingletonsInstantiated()).hasMessageContaining("documentation");
    }
    @Test void rejectsMalformedIssuer() {
        assertThatThrownBy(() -> new StagingSafetyGuard(validEnvironment().withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://")).afterSingletonsInstantiated()).hasMessageContaining("issuer");
    }
    @Test void rejectsLocalStorageAndAlwaysCleanScanner() {
        for(String property : new String[]{"app.storage.provider", "app.scanner.provider"}) {
            assertThatThrownBy(() -> new StagingSafetyGuard(validEnvironment().withProperty(property,"stub")).afterSingletonsInstantiated()).hasMessageContaining("Defender");
        }
    }
}
