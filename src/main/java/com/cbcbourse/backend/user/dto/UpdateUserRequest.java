package com.cbcbourse.backend.user.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateUserRequest(
        @NotBlank(message = "Le prenom est obligatoire") String firstName,
        @NotBlank(message = "Le nom est obligatoire") String lastName,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @NotEmpty(message = "Au moins un role doit etre assigne") Set<Long> roleIds
) {
}
