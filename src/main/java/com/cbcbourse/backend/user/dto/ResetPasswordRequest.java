package com.cbcbourse.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nouveau mot de passe a appliquer a un utilisateur")
public record ResetPasswordRequest(
        @Schema(description = "Nouveau mot de passe (8 caracteres minimum)", example = "NouveauMotDePasse123!")
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres") String newPassword
) {
}
