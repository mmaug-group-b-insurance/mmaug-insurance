package com.mmaug.claimsagent.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** The required audience identifies this API, never the calling SPA or the full scope. */
public final class ApiAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String requiredAudience;
    public ApiAudienceValidator(String requiredAudience) {
        if (requiredAudience == null || requiredAudience.isBlank()) {
            throw new IllegalArgumentException("An API audience must be configured");
        }
        this.requiredAudience = requiredAudience;
    }
    @Override public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience() != null) {
            for (String aud : token.getAudience()) {
                if (aud.equalsIgnoreCase(requiredAudience)
                        || aud.equalsIgnoreCase("api://" + requiredAudience)
                        || aud.equalsIgnoreCase("api://" + requiredAudience + "/access_as_user")) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                "Access token audience does not identify this API", null));
    }
}
