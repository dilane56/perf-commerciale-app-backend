package com.cbcbourse.backend.common.exception;

/**
 * Regle metier violee par une requete par ailleurs bien formee : les annotations de validation ne
 * peuvent pas la verifier seules (coherence entre plusieurs champs, etat de la base, etc.).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
