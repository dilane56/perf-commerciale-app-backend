package com.cbcbourse.backend.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Determine le perimetre de l'utilisateur courant sur le portefeuille commercial.
 *
 * <p>Une permission dit <i>ce qu'on a le droit de faire</i> et est verifiee par Spring Security ;
 * ce composant repond a la question complementaire <i>sur quelles lignes</i>. Un commercial ne gere
 * que les clients dont il est le referent, tandis que le responsable et la direction gerent ceux de
 * toute l'equipe. Le perimetre se deduit d'une permission, jamais d'un nom de role.
 */
@Component
public class PortfolioAccess {

    private final CurrentUser currentUser;

    public PortfolioAccess(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    /** Identifiant de l'utilisateur authentifie. */
    public Long currentUserId() {
        return currentUser.id();
    }

    /** Vrai si l'utilisateur peut agir sur le portefeuille de tous les commerciaux. */
    public boolean canManageAllPortfolios() {
        return currentUser.hasAuthority(Permissions.MANAGE_ALL_PORTFOLIOS);
    }

    /**
     * Verifie que l'utilisateur courant a le droit d'agir sur le portefeuille du commercial designe,
     * et refuse l'acces sinon.
     */
    public void checkCanActOnPortfolioOf(Long commercialReferentId) {
        if (canManageAllPortfolios()) {
            return;
        }
        if (!currentUserId().equals(commercialReferentId)) {
            throw new AccessDeniedException(
                    "Acces refuse : ce client appartient au portefeuille d'un autre commercial");
        }
    }

    /**
     * Referent a appliquer a un client cree ou modifie. Un commercial ne peut travailler que pour
     * lui-meme ; reaffecter un client a quelqu'un d'autre deplace ses chiffres dans les KPI et reste
     * donc reserve a {@code MANAGE_ALL_PORTFOLIOS}.
     */
    public Long resolveReferentId(Long demande) {
        if (!canManageAllPortfolios()) {
            if (demande != null && !demande.equals(currentUserId())) {
                throw new AccessDeniedException(
                        "Acces refuse : seul un responsable peut affecter un client a un autre commercial");
            }
            return currentUserId();
        }
        return demande != null ? demande : currentUserId();
    }
}
