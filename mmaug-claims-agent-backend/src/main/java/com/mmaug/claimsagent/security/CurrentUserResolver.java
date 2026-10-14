
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

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

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
            return new CurrentUser(
                    jwt.getSubject(),
                    jwt.getClaimAsString("oid"),
                    jwt.getClaimAsString("email"),
                    roles
            );
        }
        return null;
    }
}