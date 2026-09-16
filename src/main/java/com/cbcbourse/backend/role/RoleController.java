package com.cbcbourse.backend.role;

import java.util.List;

import com.cbcbourse.backend.role.dto.AssignPermissionsRequest;
import com.cbcbourse.backend.role.dto.CreateRoleRequest;
import com.cbcbourse.backend.role.dto.RoleResponse;
import com.cbcbourse.backend.role.dto.UpdateRoleRequest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('MANAGE_ROLES')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<RoleResponse> listRoles() {
        return roleService.findAll().stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoleResponse getRole(@PathVariable Long id) {
        return RoleResponse.from(roleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @PutMapping("/{id}")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return RoleResponse.from(roleService.update(id, request));
    }

    @PutMapping("/{id}/permissions")
    public RoleResponse assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest request) {
        return RoleResponse.from(roleService.assignPermissions(id, request.permissionIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
