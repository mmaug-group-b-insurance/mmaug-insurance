package com.mmaug.claimsagent.security;

import com.mmaug.claimsagent.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    public JwtAuthConverter(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Set<Role> extractRoles(Jwt jwt) {
        // 1. Check if token contains roles claim directly (e.g. tests or Entra ID P1/P2)
        List<String> tokenRoles = jwt.getClaimAsStringList("roles");
        if (tokenRoles != null && !tokenRoles.isEmpty()) {
            Set<Role> resolved = tokenRoles.stream()
                    .map(String::toUpperCase)
                    .filter(this::isKnownRole)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }

        // 2. Resolve roles from application database (Entra ID Free tier)
        String oid = jwt.getClaimAsString("oid");
        if (oid == null || oid.isBlank()) {
            oid = jwt.getSubject();
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("upn");
        }

        String name = jwt.getClaimAsString("name");

        if (oid != null && !oid.isBlank()) {
            try {
                Set<Role> roles = userService.getRolesForUser(oid, email, name);
                if (roles != null && !roles.isEmpty()) {
                    return roles;
                }
            } catch (Exception e) {
                log.warn("Could not resolve database roles for user oid '{}': {}", oid, e.getMessage());
            }
        }

        // Default to CLAIMS_OFFICER for any validly authenticated directory user
        return Set.of(Role.CLAIMS_OFFICER);
    }

    private boolean isKnownRole(String name) {
        for (Role r : Role.values()) {
            if (r.name().equals(name)) return true;
        }
        return false;
    }
}
