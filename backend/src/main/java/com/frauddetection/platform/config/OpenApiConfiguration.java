package com.frauddetection.platform.config;

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
public class OpenApiConfiguration {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    OpenAPI paymentFraudOpenApi() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
            .components(new Components()
                .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")))
            .info(new Info()
                .title("Payment Fraud Detection Platform API")
                .description("""
                    Explainable payment fraud decisioning API with connected payment lifecycle tracking
                    and analyst-driven fraud case workflows.
                    """)
                .version("v1")
                .contact(new Contact()
                    .name("Platform Engineering")
                    .url("https://internal.local/payment-fraud-detection-platform"))
                .license(new License()
                    .name("Internal Demo Use")));
    }
}
