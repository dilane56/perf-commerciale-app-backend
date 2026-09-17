package com.cbcbourse.backend.permission;

import java.util.List;

import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.permission.dto.PermissionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Permissions", description = "Consultation de la liste fixe des permissions systeme (definies par migration Flyway).")
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Lister les permissions", description = "Retourne toutes les permissions disponibles dans le systeme.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des permissions",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PermissionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Non authentifie",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permission MANAGE_ROLES manquante",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public List<PermissionResponse> listPermissions() {
        return permissionService.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }
}
