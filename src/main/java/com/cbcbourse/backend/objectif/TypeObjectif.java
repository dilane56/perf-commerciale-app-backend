package com.cbcbourse.backend.objectif;

/**
 * Metrique visee par un objectif, alignee sur les indicateurs deja calcules par le module KPI : un
 * objectif fixe une cible sur l'un d'entre eux, jamais une mesure inedite.
 */
public enum TypeObjectif {
    NOUVEAUX_CLIENTS,
    MONTANT_COLLECTE,
    MANDATS_SIGNES,
    ACTIVITE_TERRAIN
}
