package com.mmaug.claimsagent.support;

import com.mmaug.claimsagent.security.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class TestJwt {

    private TestJwt() {}

    public static Jwt jwtFor(String subject, Role... roles) {
        List<String> roleNames = java.util.Arrays.stream(roles)
                .map(Enum::name)
                .toList();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", subject)
                .claim("oid", subject)
                .claim("email", subject + "@example.com")
                .claim("roles", roleNames)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    public static Map<String, Object> claimsFor(String subject, Role... roles) {
        return Map.of(
                "sub", subject,
                "oid", subject,
                "email", subject + "@example.com",
                "roles", java.util.Arrays.stream(roles).map(Enum::name).toList()
        );
    }
}