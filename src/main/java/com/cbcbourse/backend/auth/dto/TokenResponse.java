package com.cbcbourse.backend.auth.dto;

import java.util.List;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String email,
        String firstName,
        String lastName,
        List<String> permissions
) {
}
