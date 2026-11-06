package com.mmaug.claimsagent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ClaimsAgentApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MmaugClaimsAgentBackendApplicationTests {

    @TempDir
    static Path storageDirectory;

    @Autowired
    DataSource dataSource;

    @Autowired
    Environment environment;

    @Autowired
    MockMvc mockMvc;

    // Keep the real security chain, without contacting an external identity provider.
    @MockitoBean
    JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void isolatedResources(DynamicPropertyRegistry registry) {
        String nativeUrl = System.getenv("MMAUG_BASELINE_DB_URL");
        if (nativeUrl != null && !nativeUrl.matches("jdbc:postgresql://127\\.0\\.0\\.1:55432/mmaug_sprint_test_[a-f0-9]+")) {
            throw new IllegalStateException("Only a uniquely named isolated baseline test database is allowed");
        }
        // Override ambient machine/CI datasource settings: never migrate a developer DB.
        registry.add("spring.datasource.url", () -> nativeUrl == null ? "jdbc:tc:postgresql:16-alpine:///mmaug_claims_test" : nativeUrl);
        registry.add("spring.datasource.driver-class-name", () -> nativeUrl == null ? "org.testcontainers.jdbc.ContainerDatabaseDriver" : "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> nativeUrl == null ? "test" : System.getenv("MMAUG_INTEGRATION_DB_USER"));
        registry.add("spring.datasource.password", () -> nativeUrl == null ? "test" : System.getenv("MMAUG_INTEGRATION_DB_PASSWORD"));
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("app.storage.base-path", () -> storageDirectory.toString());
        registry.add("app.security.enabled", () -> "true");
        registry.add("app.ai.summary-provider", () -> "deterministic");
    }

    @Test
    void contextLoadsAgainstMigratedIsolatedPostgres() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("spring.datasource.url"))
                .matches("jdbc:tc:postgresql:.*|jdbc:postgresql://127\\.0\\.0\\.1:55432/mmaug_sprint_test_[a-f0-9]+");
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var results = statement.executeQuery(
                     "SELECT count(*) FROM flyway_schema_history WHERE success = true AND type = 'SQL'")) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(results.next()).isTrue();
            assertThat(results.getInt(1)).isEqualTo(17);
        }
    }

    @Test
    void deploymentHealthProbesAreReachableWithoutExposingMetrics() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousClaimsAccessRemainsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isUnauthorized());
    }

}
