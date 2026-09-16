package com.cbcbourse.backend.permission;

import java.util.List;

import com.cbcbourse.backend.permission.dto.PermissionResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public List<PermissionResponse> listPermissions() {
        return permissionService.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }
}
