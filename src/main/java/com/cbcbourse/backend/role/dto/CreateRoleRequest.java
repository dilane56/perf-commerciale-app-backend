package com.cbcbourse.backend.role.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Donnees necessaires a la creation d'un role")
public record CreateRoleRequest(
        @Schema(description = "Nom unique du role", example = "COMMERCIAL")
        @NotBlank(message = "Le nom du role est obligatoire") String name,

        @Schema(description = "Description du role", example = "Charge de portefeuille clients")
        String description,

        @Schema(description = "Identifiants des permissions initiales (optionnel)", example = "[1, 2]")
        Set<Long> permissionIds
) {
}
