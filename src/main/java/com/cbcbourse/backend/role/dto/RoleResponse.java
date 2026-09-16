package com.cbcbourse.backend.role.dto;

import java.time.Instant;
import java.util.Set;

import com.cbcbourse.backend.permission.dto.PermissionResponse;
import com.cbcbourse.backend.role.Role;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<PermissionResponse> permissions,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoleResponse from(Role role) {
        Set<PermissionResponse> permissions = role.getPermissions().stream()
                .map(PermissionResponse::from)
                .collect(java.util.stream.Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions,
                role.getCreatedAt(), role.getUpdatedAt());
    }
}
