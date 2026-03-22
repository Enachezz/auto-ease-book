package com.api.auto_ease.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autoEaseOpenAPI() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Auto Ease API")
                        .description("""
                                REST API for the Auto Ease marketplace.

                                **Swagger UI (admins only)** requires `ROLE_ADMIN`. Use **Authorize** with a Bearer JWT from `POST /api/auth/login`, or open once `/swagger-ui.html?access_token=<admin-jwt>` to set the HttpOnly cookie (token stripped from the URL). If your React app is served from the **same origin** as the API, an admin login via `fetch(..., { credentials: 'include' })` also receives that cookie for Swagger on that host. Cross-origin SPAs typically keep the token in memory and use Bearer only.
                                """)
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components()
                        .addSecuritySchemes(bearer,
                                new SecurityScheme()
                                        .name(bearer)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
