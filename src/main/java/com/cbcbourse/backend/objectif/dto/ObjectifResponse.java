package com.cbcbourse.backend.objectif.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.cbcbourse.backend.objectif.Objectif;
import com.cbcbourse.backend.objectif.TypeObjectif;
import com.cbcbourse.backend.user.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objectif avec son avancement, calcule a la demande sur sa propre periode")
public record ObjectifResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "NOUVEAUX_CLIENTS") TypeObjectif type,
        @Schema(description = "Renseigne pour un objectif individuel", example = "2") Long commercialId,
        @Schema(example = "Awa Diallo") String commercialNom,
        @Schema(description = "Renseigne pour un objectif d'equipe", example = "3") Long managerId,
        @Schema(example = "Fatou Sow") String managerNom,
        @Schema(example = "10") BigDecimal valeurCible,
        @Schema(example = "2026-01-01") LocalDate dateDebut,
        @Schema(example = "2026-03-31") LocalDate dateFin,
        @Schema(description = "Valeur reelle sur la periode, recalculee a partir des KPI", example = "7")
        BigDecimal valeurReelle,
        @Schema(description = "Pourcentage d'atteinte (valeurReelle / valeurCible * 100)", example = "70.0")
        BigDecimal tauxAtteinte
) {
    public static ObjectifResponse of(Objectif objectif, BigDecimal valeurReelle) {
        User commercial = objectif.getCommercial();
        User manager = objectif.getManager();
        BigDecimal tauxAtteinte = valeurReelle
                .multiply(BigDecimal.valueOf(100))
                .divide(objectif.getValeurCible(), 1, RoundingMode.HALF_UP);
        return new ObjectifResponse(
                objectif.getId(),
                objectif.getType(),
                commercial != null ? commercial.getId() : null,
                commercial != null ? commercial.getFirstName() + " " + commercial.getLastName() : null,
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFirstName() + " " + manager.getLastName() : null,
                objectif.getValeurCible(),
                objectif.getDateDebut(),
                objectif.getDateFin(),
                valeurReelle,
                tauxAtteinte
        );
    }
}
