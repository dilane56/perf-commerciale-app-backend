package com.cbcbourse.backend.user;

import java.util.List;

import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.user.dto.CreateUserRequest;
import com.cbcbourse.backend.user.dto.ResetPasswordRequest;
import com.cbcbourse.backend.user.dto.UpdateUserRequest;
import com.cbcbourse.backend.user.dto.UserResponse;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs (CRUD, activation, mot de passe). Reserve a MANAGE_USERS.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Non authentifie", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Permission MANAGE_USERS manquante", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('MANAGE_USERS')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Lister les utilisateurs", description = "Retourne les utilisateurs, avec filtres optionnels par role et par statut actif/inactif.")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    @GetMapping
    public List<UserResponse> listUsers(
            @Parameter(description = "Nom du role a filtrer", example = "COMMERCIAL") @RequestParam(required = false) String role,
            @Parameter(description = "Filtrer sur le statut actif/inactif") @RequestParam(required = false) Boolean active) {
        return userService.findAll(role, active).stream().map(UserResponse::from).toList();
    }

    @Operation(summary = "Consulter un utilisateur", description = "Retourne le detail d'un utilisateur par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur trouve",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public UserResponse getUser(@Parameter(description = "Identifiant de l'utilisateur") @PathVariable Long id) {
        return UserResponse.from(userService.findById(id));
    }

    @Operation(summary = "Creer un utilisateur", description = "Cree un nouvel utilisateur avec un ou plusieurs roles assignes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur cree",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email deja utilise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @Operation(summary = "Modifier un utilisateur", description = "Met a jour les informations et les roles assignes d'un utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur mis a jour",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email deja utilise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public UserResponse updateUser(@Parameter(description = "Identifiant de l'utilisateur") @PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    @Operation(summary = "Desactiver un utilisateur", description = "Desactive le compte sans le supprimer (conserve l'historique).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur desactive",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivateUser(@Parameter(description = "Identifiant de l'utilisateur") @PathVariable Long id) {
        return UserResponse.from(userService.setActive(id, false));
    }

    @Operation(summary = "Reactiver un utilisateur", description = "Reactive un compte precedemment desactive.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur reactive",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/reactivate")
    public UserResponse reactivateUser(@Parameter(description = "Identifiant de l'utilisateur") @PathVariable Long id) {
        return UserResponse.from(userService.setActive(id, true));
    }

    @Operation(summary = "Reinitialiser le mot de passe", description = "Definit un nouveau mot de passe pour l'utilisateur (usage administrateur).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mot de passe reinitialise"),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@Parameter(description = "Identifiant de l'utilisateur") @PathVariable Long id,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
