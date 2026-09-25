package com.cbcbourse.backend.client.dto;

import java.time.LocalDate;

import com.cbcbourse.backend.client.StatutClient;
import com.cbcbourse.backend.client.TypeClient;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Modification d'un client ou d'un prospect")
public record UpdateClientRequest(
        @NotNull(message = "Le type de client est obligatoire") TypeClient type,
        @Size(max = 255) String nom,
        @Size(max = 255) String raisonSociale,

        @Schema(description = "Nouveau commercial referent. Reserve a MANAGE_ALL_PORTFOLIOS : "
                + "reaffecter un client deplace ses chiffres dans les KPI.", example = "3")
        Long commercialReferentId,

        @NotNull(message = "La date d'acquisition est obligatoire") LocalDate dateAcquisition,
        @NotNull(message = "Le statut est obligatoire") StatutClient statut
) {
}
