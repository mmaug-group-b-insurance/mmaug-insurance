
package com.mmaug.claimsagent.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.mmaug.claimsagent.common.exception.ForbiddenException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.delegated-scope:mmaug_insurance_app}")
    private String delegatedScope;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // ------------------------------------------------------------------
        // Dev mode: security is disabled. Return a synthetic user with all
        // roles so controllers, services, and audit logging can proceed.
        //
        // This branch never runs in production because application-local.yml
        // sets app.security.enabled=false only for the local profile.
        // ------------------------------------------------------------------
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
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
            String scopes = jwt.getClaimAsString("scp");
            if (scopes == null || !List.of(scopes.split(" ")).contains(delegatedScope)) {
                throw new ForbiddenException("The delegated claims API scope is required");
            }
            String oid = jwt.getClaimAsString("oid");
            UUID actor;
            try {
                actor = UUID.fromString(oid);
            } catch (IllegalArgumentException | NullPointerException invalid) {
                throw new ForbiddenException("An officer directory object ID is required");
            }
            if (!actor.toString().equalsIgnoreCase(oid)) throw new ForbiddenException("Invalid directory object ID");
            return new CurrentUser(
                    actor.toString(),
                    oid,
                    jwt.getClaimAsString("email"),
                    roles
            );
        }
        return null;
    }
}