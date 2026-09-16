package com.cbcbourse.backend.auth.dto;

/**
 * Security-context principal built directly from a validated access token's claims,
 * so requests are authorized without hitting the database on every call.
 */
public record AuthenticatedUser(Long id, String email) {
}
