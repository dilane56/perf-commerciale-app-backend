package com.cbcbourse.backend.transaction;

/**
 * Origine de la donnee. Permet de tracer, une fois Atlantis SGI branche, quelles transactions
 * proviennent de la saisie manuelle et lesquelles sont importees.
 */
public enum SourceSysteme {
    MANUEL,
    ATLANTIS
}
