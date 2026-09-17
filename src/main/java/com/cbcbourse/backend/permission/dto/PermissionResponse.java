package com.cbcbourse.backend.permission.dto;

import com.cbcbourse.backend.permission.Permission;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation d'une permission systeme (liste fixe, definie par migration Flyway)")
public record PermissionResponse(
        @Schema(description = "Identifiant technique", example = "1") Long id,
        @Schema(description = "Code unique de la permission", example = "MANAGE_USERS") String code,
        @Schema(description = "Description de la permission", example = "Creer/modifier/desactiver des utilisateurs") String description
) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
    }
}
