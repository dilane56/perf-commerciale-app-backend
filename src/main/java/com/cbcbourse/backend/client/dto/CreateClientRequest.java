package com.cbcbourse.backend.client.dto;

import java.time.LocalDate;

import com.cbcbourse.backend.client.StatutClient;
import com.cbcbourse.backend.client.TypeClient;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Creation d'un client ou d'un prospect")
public record CreateClientRequest(
        @Schema(description = "Nature juridique", example = "PERSONNE_PHYSIQUE")
        @NotNull(message = "Le type de client est obligatoire") TypeClient type,

        @Schema(description = "Nom et prenom, pour une personne physique", example = "Ibrahim Traore")
        @Size(max = 255) String nom,

        @Schema(description = "Denomination sociale, pour une personne morale", example = "SODIMA SA")
        @Size(max = 255) String raisonSociale,

        @Schema(description = "Commercial referent. Ignore pour un commercial, qui ne peut creer que pour lui-meme.",
                example = "2") Long commercialReferentId,

        @Schema(description = "Date d'entree en relation", example = "2026-01-15")
        @NotNull(message = "La date d'acquisition est obligatoire") LocalDate dateAcquisition,

        @Schema(description = "Statut commercial", example = "PROSPECT")
        @NotNull(message = "Le statut est obligatoire") StatutClient statut
) {
}
