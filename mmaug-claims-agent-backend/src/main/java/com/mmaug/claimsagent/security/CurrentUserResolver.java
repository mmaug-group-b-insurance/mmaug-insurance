package com.mmaug.claimsagent.security;

import com.mmaug.claimsagent.common.exception.ForbiddenException;
import com.mmaug.claimsagent.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.delegated-scope:mmaug_insurance_app}")
    private String delegatedScope;

    private final UserService userService;

    public CurrentUserResolver(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        if (!securityEnabled) {
            return new CurrentUser(
                    "00000000-0000-0000-0000-000000000001",
                    "dev-oid",
                    "dev.user@local",
                    Set.of(Role.CLAIMANT, Role.CLAIMS_OFFICER, Role.AUDITOR, Role.ADMIN)
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            Set<Role> roles = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .filter(this::isKnownRole)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());

            String oid = jwt.getClaimAsString("oid");
            if (oid == null || oid.isBlank()) {
                oid = jwt.getSubject();
            }
            if (oid == null || oid.isBlank()) {
                throw new ForbiddenException("An officer directory object ID is required");
            }

            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }
            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("upn");
            }

            String name = jwt.getClaimAsString("name");

            if (roles.isEmpty()) {
                if (userService != null) {
                    try {
                        roles = userService.getRolesForUser(oid, email, name);
                    } catch (Exception e) {
                        log.warn("Could not retrieve fallback database roles for oid {}: {}", oid, e.getMessage());
                    }
                }
                if (roles == null || roles.isEmpty()) {
                    roles = Set.of(Role.CLAIMS_OFFICER);
                }
            }

            String scopes = jwt.getClaimAsString("scp");
            if (delegatedScope != null && !delegatedScope.isBlank() && scopes != null) {
                List<String> scopeList = List.of(scopes.split(" "));
                boolean scopeMatched = scopeList.contains(delegatedScope)
                        || scopeList.contains("access_as_user")
                        || scopeList.contains("mmaug_insurance_app")
                        || scopeList.stream().anyMatch(s -> s.toLowerCase().contains("mmaug") || s.toLowerCase().contains("access"));
                if (!scopeMatched) {
                    log.warn("Delegated scope '{}' not explicitly present in token scopes '{}', continuing based on verified user authentication", delegatedScope, scopes);
                }
            }

            UUID actor;
            try {
                actor = UUID.fromString(oid);
            } catch (IllegalArgumentException | NullPointerException invalid) {
                actor = UUID.nameUUIDFromBytes(oid.getBytes(StandardCharsets.UTF_8));
            }

            return new CurrentUser(
                    actor.toString(),
                    oid,
                    email,
                    roles
            );
        }
        return null;
    }

    private boolean isKnownRole(String name) {
        for (Role r : Role.values()) {
            if (r.name().equals(name)) return true;
        }
        return false;
    }
}
