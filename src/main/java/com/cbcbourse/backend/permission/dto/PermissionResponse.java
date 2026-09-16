package com.cbcbourse.backend.permission.dto;

import com.cbcbourse.backend.permission.Permission;

public record PermissionResponse(Long id, String code, String description) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
    }
}
