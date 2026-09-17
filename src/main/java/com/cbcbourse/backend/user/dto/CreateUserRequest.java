package com.cbcbourse.backend.user.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "Donnees necessaires a la creation d'un utilisateur")
public record CreateUserRequest(
        @Schema(description = "Prenom", example = "Awa")
        @NotBlank(message = "Le prenom est obligatoire") String firstName,

        @Schema(description = "Nom", example = "Diallo")
        @NotBlank(message = "Le nom est obligatoire") String lastName,

        @Schema(description = "Email, sert d'identifiant de connexion", example = "awa.diallo@example.com")
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,

        @Schema(description = "Mot de passe initial (8 caracteres minimum)", example = "MotDePasse123!")
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres") String password,

        @Schema(description = "Identifiants des roles a assigner (au moins un)", example = "[1]")
        @NotEmpty(message = "Au moins un role doit etre assigne") Set<Long> roleIds
) {
}
