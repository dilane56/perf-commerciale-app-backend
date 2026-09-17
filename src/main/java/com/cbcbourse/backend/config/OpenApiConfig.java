package com.cbcbourse.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Performance Commerciale API",
                version = "v1",
                description = "API du module Utilisateurs, Roles & Permissions (RBAC). "
                        + "Fournit l'authentification JWT et la gestion des comptes utilisateurs, roles et permissions "
                        + "pour l'application de performance commerciale.",
                contact = @Contact(name = "Equipe technique", email = "kamgaingfotso@gmail.com")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Recuperer l'accessToken via POST /api/auth/login, puis cliquer sur \"Authorize\" et coller le token (sans le prefixe Bearer)."
)
public class OpenApiConfig {
}
