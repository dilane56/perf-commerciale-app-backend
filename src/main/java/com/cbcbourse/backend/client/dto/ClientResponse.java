package com.cbcbourse.backend.client.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cbcbourse.backend.client.Client;
import com.cbcbourse.backend.client.StatutClient;
import com.cbcbourse.backend.client.TypeClient;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Client ou prospect du portefeuille commercial")
public record ClientResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "PERSONNE_PHYSIQUE") TypeClient type,
        @Schema(example = "Ibrahim Traore") String nom,
        @Schema(example = "SODIMA SA") String raisonSociale,
        @Schema(description = "Libelle d'affichage, quel que soit le type", example = "Ibrahim Traore") String designation,
        @Schema(description = "Identifiant du commercial referent", example = "2") Long commercialReferentId,
        @Schema(description = "Nom du commercial referent", example = "Awa Diallo") String commercialReferentNom,
        @Schema(example = "2026-01-15") LocalDate dateAcquisition,
        @Schema(example = "CLIENT_ACTIF") StatutClient statut,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClientResponse from(Client client) {
        var referent = client.getCommercialReferent();
        return new ClientResponse(
                client.getId(),
                client.getType(),
                client.getNom(),
                client.getRaisonSociale(),
                client.designation(),
                referent.getId(),
                referent.getFirstName() + " " + referent.getLastName(),
                client.getDateAcquisition(),
                client.getStatut(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
