package com.cbcbourse.backend.common.security;

/**
 * Codes des permissions reconnues par le code. Ils doivent correspondre exactement aux lignes de la
 * table {@code permissions} alimentee par les migrations Flyway : une permission absente de cette
 * liste ne serait verifiee nulle part, une constante sans ligne en base ne serait jamais accordee.
 */
public final class Permissions {

    public static final String MANAGE_USERS = "MANAGE_USERS";
    public static final String MANAGE_ROLES = "MANAGE_ROLES";
    public static final String VIEW_OWN_DASHBOARD = "VIEW_OWN_DASHBOARD";
    public static final String VIEW_TEAM_DASHBOARD = "VIEW_TEAM_DASHBOARD";
    public static final String VIEW_ALL_DASHBOARDS = "VIEW_ALL_DASHBOARDS";
    public static final String MANAGE_OBJECTIVES = "MANAGE_OBJECTIVES";
    public static final String EXPORT_REPORTS = "EXPORT_REPORTS";

    /** Gerer ses propres clients/prospects et leurs rendez-vous. */
    public static final String MANAGE_OWN_PORTFOLIO = "MANAGE_OWN_PORTFOLIO";
    /** Gerer les clients et rendez-vous de toute l'equipe commerciale. */
    public static final String MANAGE_ALL_PORTFOLIOS = "MANAGE_ALL_PORTFOLIOS";
    /** Supprimer une saisie erronee plutot que la corriger. */
    public static final String DELETE_PORTFOLIO_DATA = "DELETE_PORTFOLIO_DATA";

    /**
     * Fixer les objectifs de tous les commerciaux et de toutes les equipes. MANAGE_OBJECTIVES reste
     * limite a sa propre equipe (verifie par {@link ObjectifAccess}) ; cette permission etend ce
     * perimetre, sur le meme principe que MANAGE_ALL_PORTFOLIOS pour le portefeuille.
     */
    public static final String MANAGE_ALL_OBJECTIVES = "MANAGE_ALL_OBJECTIVES";

    private Permissions() {
    }
}
