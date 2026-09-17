package com.cbcbourse.backend.role.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nouvel ensemble de permissions a assigner a un role (remplace l'ensemble existant)")
public record AssignPermissionsRequest(
        @Schema(description = "Identifiants des permissions a assigner (liste vide autorisee pour tout retirer)", example = "[1, 3, 5]")
        @NotNull(message = "La liste des permissions est obligatoire") Set<Long> permissionIds
) {
}
