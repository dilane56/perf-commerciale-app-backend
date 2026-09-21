package com.cbcbourse.backend.kpi.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicateurs de performance d'un commercial sur une periode")
public record KpiCommercialResponse(
        @Schema(description = "Identifiant du commercial", example = "2") Long userId,
        @Schema(description = "Nom complet du commercial", example = "Awa Diallo") String nomComplet,
        @Schema(description = "Email du commercial", example = "awa.diallo@cbcbourse.local") String email,
        @Schema(description = "Clients dont il est referent et acquis sur la periode", example = "4") long nouveauxClients,
        @Schema(description = "Montant collecte en XOF sur les clients dont il est referent", example = "12500000.00") BigDecimal montantCollecte,
        @Schema(description = "Mandats de gestion signes sur la periode", example = "2") long mandatsSignes,
        @Schema(description = "Rendez-vous auxquels il a participe, meme sans etre referent du client", example = "11") long rendezVous
) {
}
