package com.cbcbourse.backend.client;

/** Etape du cycle de vie commercial : un prospect devient client actif, puis eventuellement inactif. */
public enum StatutClient {
    PROSPECT,
    CLIENT_ACTIF,
    CLIENT_INACTIF
}
