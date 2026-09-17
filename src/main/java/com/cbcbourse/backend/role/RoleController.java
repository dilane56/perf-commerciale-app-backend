package com.cbcbourse.backend.role;

import java.util.List;

import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.role.dto.AssignPermissionsRequest;
import com.cbcbourse.backend.role.dto.CreateRoleRequest;
import com.cbcbourse.backend.role.dto.RoleResponse;
import com.cbcbourse.backend.role.dto.UpdateRoleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Roles", description = "Gestion des roles et de leurs permissions. Reserve a MANAGE_ROLES.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Non authentifie", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Permission MANAGE_ROLES manquante", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('MANAGE_ROLES')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Lister les roles", description = "Retourne tous les roles avec leurs permissions assignees.")
    @ApiResponse(responseCode = "200", description = "Liste des roles",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResponse.class))))
    @GetMapping
    public List<RoleResponse> listRoles() {
        return roleService.findAll().stream().map(RoleResponse::from).toList();
    }

    @Operation(summary = "Consulter un role", description = "Retourne le detail d'un role et de ses permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role trouve",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public RoleResponse getRole(@Parameter(description = "Identifiant du role") @PathVariable Long id) {
        return RoleResponse.from(roleService.findById(id));
    }

    @Operation(summary = "Creer un role", description = "Cree un nouveau role, avec des permissions initiales optionnelles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role cree",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Nom de role deja utilise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @Operation(summary = "Modifier un role", description = "Met a jour le nom et la description d'un role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role mis a jour",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Role introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Nom de role deja utilise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public RoleResponse updateRole(@Parameter(description = "Identifiant du role") @PathVariable Long id,
                                    @Valid @RequestBody UpdateRoleRequest request) {
        return RoleResponse.from(roleService.update(id, request));
    }

    @Operation(summary = "Assigner des permissions", description = "Remplace l'ensemble des permissions assignees a un role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions mises a jour",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Role ou permission introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/permissions")
    public RoleResponse assignPermissions(@Parameter(description = "Identifiant du role") @PathVariable Long id,
                                           @Valid @RequestBody AssignPermissionsRequest request) {
        return RoleResponse.from(roleService.assignPermissions(id, request.permissionIds()));
    }

    @Operation(summary = "Supprimer un role", description = "Supprime un role. Refuse si le role est encore assigne a des utilisateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role supprime"),
            @ApiResponse(responseCode = "404", description = "Role introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Role encore assigne a des utilisateurs",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@Parameter(description = "Identifiant du role") @PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
