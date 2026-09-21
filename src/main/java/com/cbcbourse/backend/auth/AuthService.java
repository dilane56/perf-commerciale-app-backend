package com.cbcbourse.backend.auth;

import com.cbcbourse.backend.auth.dto.CurrentUserResponse;
import com.cbcbourse.backend.auth.dto.LoginRequest;
import com.cbcbourse.backend.auth.dto.TokenResponse;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserPrincipal;
import com.cbcbourse.backend.user.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                        UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public TokenResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildTokenResponse(principal.getUser());
    }

    /**
     * Relit le profil et les permissions depuis la base : le frontend peut ainsi restaurer
     * la session apres un rechargement de page sans se fier au contenu (potentiellement perime)
     * de l'access token.
     */
    public CurrentUserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return CurrentUserResponse.from(user);
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Refresh token invalide ou expire");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Le token fourni n'est pas un refresh token");
        }

        Long userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!user.isActive()) {
            throw new DisabledException("Compte desactive");
        }

        return buildTokenResponse(user);
    }

    private TokenResponse buildTokenResponse(User user) {
        var permissionCodes = new UserPrincipal(user).permissionCodes();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), permissionCodes);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                permissionCodes
        );
    }
}
