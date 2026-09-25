package com.cbcbourse.backend.common.security;

import com.cbcbourse.backend.auth.dto.AuthenticatedUser;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acces a l'utilisateur authentifie courant, partage par les composants qui appliquent un
 * cloisonnement au-dela de la permission deja verifiee par Spring Security (portefeuille, objectifs).
 */
@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AccessDeniedException("Aucun utilisateur authentifie");
        }
        return principal.id();
    }

    public boolean hasAuthority(String code) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(code::equals);
    }
}
