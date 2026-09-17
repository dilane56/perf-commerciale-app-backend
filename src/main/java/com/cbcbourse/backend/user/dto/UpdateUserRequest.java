package com.cbcbourse.backend.user.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Donnees modifiables d'un utilisateur existant")
public record UpdateUserRequest(
        @Schema(description = "Prenom", example = "Awa")
        @NotBlank(message = "Le prenom est obligatoire") String firstName,

        @Schema(description = "Nom", example = "Diallo")
        @NotBlank(message = "Le nom est obligatoire") String lastName,

        @Schema(description = "Email, sert d'identifiant de connexion", example = "awa.diallo@example.com")
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,

        @Schema(description = "Identifiants des roles assignes (au moins un)", example = "[1]")
        @NotEmpty(message = "Au moins un role doit etre assigne") Set<Long> roleIds
) {
}
