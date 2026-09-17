package com.cbcbourse.backend.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import com.cbcbourse.backend.user.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation d'un utilisateur")
public record UserResponse(
        @Schema(description = "Identifiant technique", example = "1") Long id,
        @Schema(description = "Prenom", example = "Awa") String firstName,
        @Schema(description = "Nom", example = "Diallo") String lastName,
        @Schema(description = "Email / identifiant de connexion", example = "awa.diallo@example.com") String email,
        @Schema(description = "Compte actif ou desactive", example = "true") boolean active,
        @Schema(description = "Noms des roles assignes", example = "[\"COMMERCIAL\"]") Set<String> roles,
        @Schema(description = "Date de creation (UTC)") Instant createdAt,
        @Schema(description = "Date de derniere modification (UTC)") Instant updatedAt
) {
    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isActive(), roleNames, user.getCreatedAt(), user.getUpdatedAt());
    }
}
