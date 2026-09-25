package com.cbcbourse.backend.objectif.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cbcbourse.backend.objectif.TypeObjectif;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Fixation d'un objectif. Exactement un des deux titulaires (commercialId pour un "
        + "objectif individuel, managerId pour un objectif d'equipe) doit etre renseigne.")
public record CreateObjectifRequest(
        @Schema(description = "Metrique visee", example = "NOUVEAUX_CLIENTS")
        @NotNull(message = "Le type d'objectif est obligatoire") TypeObjectif type,

        @Schema(description = "Commercial vise, pour un objectif individuel", example = "2") Long commercialId,

        @Schema(description = "Responsable dont l'equipe est visee, pour un objectif d'equipe", example = "3")
        Long managerId,

        @Schema(description = "Valeur a atteindre sur la periode", example = "10")
        @NotNull(message = "La valeur cible est obligatoire")
        @DecimalMin(value = "0.01", message = "La valeur cible doit etre strictement positive") BigDecimal valeurCible,

        @Schema(description = "Debut de periode (inclus)", example = "2026-01-01")
        @NotNull(message = "La date de debut est obligatoire") LocalDate dateDebut,

        @Schema(description = "Fin de periode (incluse)", example = "2026-03-31")
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin
) {
}
