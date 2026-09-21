package com.cbcbourse.backend.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vue consolidee : total de l'equipe et detail par commercial sur une periode")
public record KpiPeriodeResponse(
        @Schema(description = "Debut de la periode (inclus)", example = "2026-01-01") LocalDate debut,
        @Schema(description = "Fin de la periode (incluse)", example = "2026-12-31") LocalDate fin,
        @Schema(description = "Nouveaux clients de l'equipe", example = "9") long totalNouveauxClients,
        @Schema(description = "Montant total collecte en XOF", example = "31500000.00") BigDecimal totalMontantCollecte,
        @Schema(description = "Mandats de gestion signes par l'equipe", example = "5") long totalMandatsSignes,
        @Schema(description = "Detail commercial par commercial") List<KpiCommercialResponse> parCommercial
) {
    public static KpiPeriodeResponse of(LocalDate debut, LocalDate fin, List<KpiCommercialResponse> parCommercial) {
        // Les totaux somment les KPI individuels : comme chaque client a un referent unique, aucun
        // client ni aucun montant n'est compte deux fois. Les rendez-vous, eux, ne sont pas totalises :
        // une meme visite en binome apparait chez chaque participant et gonflerait le total.
        long nouveauxClients = parCommercial.stream().mapToLong(KpiCommercialResponse::nouveauxClients).sum();
        long mandats = parCommercial.stream().mapToLong(KpiCommercialResponse::mandatsSignes).sum();
        BigDecimal montant = parCommercial.stream()
                .map(KpiCommercialResponse::montantCollecte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new KpiPeriodeResponse(debut, fin, nouveauxClients, montant, mandats, parCommercial);
    }
}
