package com.cbcbourse.backend.role.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record AssignPermissionsRequest(
        @NotNull(message = "La liste des permissions est obligatoire") Set<Long> permissionIds
) {
}
