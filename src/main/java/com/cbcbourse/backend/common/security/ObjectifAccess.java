package com.cbcbourse.backend.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Determine le perimetre de l'utilisateur courant sur les objectifs commerciaux.
 *
 * <p>MANAGE_OBJECTIVES autorise a fixer des objectifs, mais seulement pour sa propre equipe ; seule
 * MANAGE_ALL_OBJECTIVES etend ce perimetre a tous les commerciaux et equipes. Meme principe que pour
 * le portefeuille : la permission dit ce qu'on a le droit de faire, ce composant dit sur quelles lignes.
 */
@Component
public class ObjectifAccess {

    private final CurrentUser currentUser;

    public ObjectifAccess(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public Long currentUserId() {
        return currentUser.id();
    }

    public boolean canManageAllObjectives() {
        return currentUser.hasAuthority(Permissions.MANAGE_ALL_OBJECTIVES);
    }

    /**
     * Verifie qu'un objectif individuel peut etre fixe pour un commercial dont le responsable est
     * designe par {@code managerIdDuCommercial}. Un commercial sans responsable assigne n'appartient
     * a l'equipe de personne : seule MANAGE_ALL_OBJECTIVES peut alors agir pour lui.
     */
    public void checkCanManageObjectiveOfCommercial(Long managerIdDuCommercial) {
        if (canManageAllObjectives()) {
            return;
        }
        if (managerIdDuCommercial == null || !currentUserId().equals(managerIdDuCommercial)) {
            throw new AccessDeniedException(
                    "Acces refuse : ce commercial ne fait pas partie de votre equipe");
        }
    }

    /** Verifie qu'un objectif d'equipe peut etre fixe pour l'equipe du responsable designe. */
    public void checkCanManageObjectiveOfTeam(Long managerId) {
        if (canManageAllObjectives()) {
            return;
        }
        if (!currentUserId().equals(managerId)) {
            throw new AccessDeniedException(
                    "Acces refuse : seule la direction peut fixer un objectif pour l'equipe d'un autre responsable");
        }
    }
}
