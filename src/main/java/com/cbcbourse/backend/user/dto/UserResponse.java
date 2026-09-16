package com.cbcbourse.backend.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import com.cbcbourse.backend.user.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean active,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isActive(), roleNames, user.getCreatedAt(), user.getUpdatedAt());
    }
}
