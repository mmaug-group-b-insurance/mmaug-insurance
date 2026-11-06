package com.mmaug.claimsagent.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ApiAudienceValidatorTest {
    private static final String API = "09ad3a12-27c0-41eb-a6c1-ac8d7bfaa98f"; // gitleaks:allow -- public application ID, not a credential
    private final ApiAudienceValidator validator = new ApiAudienceValidator(API);
    private Jwt token(String audience) {
        return Jwt.withTokenValue("synthetic-test-token").header("alg", "RS256")
                .subject("synthetic-officer").audience(List.of(audience)).build();
    }
    @Test void acceptsTheResourceApiAudience() {
        assertThat(validator.validate(token(API)).hasErrors()).isFalse();
    }
    @Test void rejectsMicrosoftGraphAudience() {
        assertThat(validator.validate(token("00000003-0000-0000-c000-000000000000")).hasErrors()).isTrue();
    }
    @Test void rejectsTheSpaAndFullScopeAsAudiences() {
        assertThat(validator.validate(token("synthetic-spa-client-id")).hasErrors()).isTrue();
        assertThat(validator.validate(token("api://" + API + "/access_as_user/mmaug_insurance_app")).hasErrors()).isTrue();
    }
    @Test void rejectsMissingAudience() {
        Jwt token = Jwt.withTokenValue("synthetic").header("alg", "RS256").subject("synthetic-officer").build();
        assertThat(validator.validate(token).hasErrors()).isTrue();
    }
    @Test void rejectsBlankConfiguration() {
        assertThatThrownBy(() -> new ApiAudienceValidator(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
