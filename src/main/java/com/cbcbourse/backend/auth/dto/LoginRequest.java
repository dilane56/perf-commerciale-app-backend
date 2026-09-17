package com.cbcbourse.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Identifiants de connexion")
public record LoginRequest(
        @Schema(description = "Email de l'utilisateur", example = "admin@perfcommerciale.com")
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,

        @Schema(description = "Mot de passe", example = "MotDePasse123!")
        @NotBlank(message = "Le mot de passe est obligatoire") String password
) {
}
