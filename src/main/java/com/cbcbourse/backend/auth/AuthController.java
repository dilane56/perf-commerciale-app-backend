package com.cbcbourse.backend.auth;

import com.cbcbourse.backend.auth.dto.LoginRequest;
import com.cbcbourse.backend.auth.dto.RefreshTokenRequest;
import com.cbcbourse.backend.auth.dto.TokenResponse;
import com.cbcbourse.backend.common.exception.ApiError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Connexion, rafraichissement et deconnexion (JWT stateless). Endpoints publics.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Connexion", description = "Authentifie un utilisateur par email + mot de passe et retourne un access token "
            + "et un refresh token JWT, ainsi que les permissions de l'utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion reussie",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Rafraichir le token", description = "Genere une nouvelle paire access/refresh token a partir d'un refresh token valide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rafraichi",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expire",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Deconnexion", description = "V1 stateless : invalide les tokens cote client uniquement "
            + "(pas de blacklist serveur pour le moment).")
    @ApiResponse(responseCode = "204", description = "Deconnexion effectuee")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // v1 stateless : le client supprime ses tokens localement. Une blacklist pourra etre ajoutee plus tard.
        return ResponseEntity.noContent().build();
    }
}
