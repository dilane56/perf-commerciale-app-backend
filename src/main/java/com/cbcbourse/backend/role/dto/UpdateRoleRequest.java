package com.cbcbourse.backend.role.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank(message = "Le nom du role est obligatoire") String name,
        String description
) {
}
