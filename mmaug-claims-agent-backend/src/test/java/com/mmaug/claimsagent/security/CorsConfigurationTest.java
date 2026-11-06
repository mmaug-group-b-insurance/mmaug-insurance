package com.mmaug.claimsagent.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsConfigurationTest {
    @Test void usesConfiguredOriginAndRejectsOtherOrigins() {
        var environment = new MockEnvironment().withProperty("app.security.cors.allowed-origins[0]", "https://claims.example.test");
        var source = new SecurityConfig(mock(JwtAuthConverter.class), environment).corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/claims"));
        assertThat(config.checkOrigin("https://claims.example.test")).isEqualTo("https://claims.example.test");
        assertThat(config.checkOrigin("http://localhost:5173")).isNull();
        assertThat(config.checkOrigin("https://attacker.example.test")).isNull();
        assertThat(config.getExposedHeaders()).contains("X-Correlation-Id");
    }
    @Test void absentConfigurationDoesNotAllowArbitraryOrigins() {
        var source = new SecurityConfig(mock(JwtAuthConverter.class), new MockEnvironment()).corsConfigurationSource();
        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/claims")).checkOrigin("https://claims.example.test")).isNull();
    }
}
