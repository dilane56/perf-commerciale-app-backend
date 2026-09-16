package com.cbcbourse.backend.auth;

import com.cbcbourse.backend.auth.dto.LoginRequest;
import com.cbcbourse.backend.auth.dto.RefreshTokenRequest;
import com.cbcbourse.backend.auth.dto.TokenResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // v1 stateless : le client supprime ses tokens localement. Une blacklist pourra etre ajoutee plus tard.
        return ResponseEntity.noContent().build();
    }
}
