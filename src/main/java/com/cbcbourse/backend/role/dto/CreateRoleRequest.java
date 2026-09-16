package com.cbcbourse.backend.role.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank(message = "Le nom du role est obligatoire") String name,
        String description,
        Set<Long> permissionIds
) {
}
