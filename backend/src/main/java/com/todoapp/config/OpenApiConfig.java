package com.todoapp.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 *
 * <p>Defines a JWT Bearer security scheme named {@code bearerAuth} and applies
 * it as a global security requirement so every endpoint in the Swagger UI
 * shows the lock icon and accepts a Bearer token.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Todo App API")
                        .version("1.0.0")
                        .description("REST API for the collaborative To-Do web application"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
