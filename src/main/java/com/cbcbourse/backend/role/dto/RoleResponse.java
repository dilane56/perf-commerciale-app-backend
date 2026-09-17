package com.cbcbourse.backend.role.dto;

import java.time.Instant;
import java.util.Set;

import com.cbcbourse.backend.permission.dto.PermissionResponse;
import com.cbcbourse.backend.role.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation d'un role avec ses permissions")
public record RoleResponse(
        @Schema(description = "Identifiant technique", example = "1") Long id,
        @Schema(description = "Nom unique du role", example = "COMMERCIAL") String name,
        @Schema(description = "Description du role", example = "Charge de portefeuille clients") String description,
        @Schema(description = "Permissions assignees au role") Set<PermissionResponse> permissions,
        @Schema(description = "Date de creation (UTC)") Instant createdAt,
        @Schema(description = "Date de derniere modification (UTC)") Instant updatedAt
) {
    public static RoleResponse from(Role role) {
        Set<PermissionResponse> permissions = role.getPermissions().stream()
                .map(PermissionResponse::from)
                .collect(java.util.stream.Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions,
                role.getCreatedAt(), role.getUpdatedAt());
    }
}
