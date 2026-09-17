package com.cbcbourse.backend.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Donnees modifiables d'un role existant (nom, description)")
public record UpdateRoleRequest(
        @Schema(description = "Nom unique du role", example = "COMMERCIAL")
        @NotBlank(message = "Le nom du role est obligatoire") String name,

        @Schema(description = "Description du role", example = "Charge de portefeuille clients")
        String description
) {
}
