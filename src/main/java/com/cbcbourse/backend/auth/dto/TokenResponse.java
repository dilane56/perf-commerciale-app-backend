package com.cbcbourse.backend.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paire de tokens JWT et informations de l'utilisateur authentifie")
public record TokenResponse(
        @Schema(description = "Token JWT a utiliser dans l'en-tete Authorization (Bearer)") String accessToken,
        @Schema(description = "Token a utiliser sur POST /api/auth/refresh pour obtenir un nouvel access token") String refreshToken,
        @Schema(description = "Type de token", example = "Bearer") String tokenType,
        @Schema(description = "Identifiant de l'utilisateur", example = "1") Long userId,
        @Schema(description = "Email de l'utilisateur", example = "admin@perfcommerciale.com") String email,
        @Schema(description = "Prenom de l'utilisateur", example = "Admin") String firstName,
        @Schema(description = "Nom de l'utilisateur", example = "Systeme") String lastName,
        @Schema(description = "Codes des permissions accordees a l'utilisateur", example = "[\"MANAGE_USERS\", \"MANAGE_ROLES\"]") List<String> permissions
) {
}
