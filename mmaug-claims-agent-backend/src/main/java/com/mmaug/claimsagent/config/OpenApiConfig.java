package com.mmaug.claimsagent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MMAUG Insurance Claims Agent API")
                        .version("v1")
                        .description("""
                                REST API for the MMAUG Insurance Claims Agent (RFP MMAUG-AIDB-RFP-003).
                                The system provides AI-assisted claims processing with human oversight
                                and auditable workflows.
                                
                                Responsible AI: this system is decision-support only. It never
                                approves, rejects, determines liability, authorises payment,
                                or modifies a policy.
                                """)
                        .contact(new Contact()
                                .name("MMAUG Insurance Ltd")
                                .url("https://www.mmaug.example"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.mmaug.example/license")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter a valid Microsoft Entra ID JWT token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}