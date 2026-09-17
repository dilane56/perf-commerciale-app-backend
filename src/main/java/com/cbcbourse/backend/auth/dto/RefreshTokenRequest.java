package com.cbcbourse.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token a echanger contre une nouvelle paire de tokens")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token obtenu lors de la connexion")
        @NotBlank(message = "Le refresh token est obligatoire") String refreshToken
) {
}
