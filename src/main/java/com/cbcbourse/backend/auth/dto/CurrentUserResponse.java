package com.cbcbourse.backend.auth.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cbcbourse.backend.role.Role;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserPrincipal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Profil de l'utilisateur authentifie, avec ses permissions effectives")
public record CurrentUserResponse(
        @Schema(description = "Identifiant de l'utilisateur", example = "1") Long id,
        @Schema(description = "Email / identifiant de connexion", example = "admin@cbcbourse.local") String email,
        @Schema(description = "Prenom", example = "Admin") String firstName,
        @Schema(description = "Nom", example = "Systeme") String lastName,
        @Schema(description = "Compte actif", example = "true") boolean active,
        @Schema(description = "Noms des roles assignes", example = "[\"ADMIN\"]") Set<String> roles,
        @Schema(description = "Codes des permissions accordees", example = "[\"MANAGE_USERS\", \"MANAGE_ROLES\"]") List<String> permissions
) {
    public static CurrentUserResponse from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.isActive(), roleNames, new UserPrincipal(user).permissionCodes());
    }
}
