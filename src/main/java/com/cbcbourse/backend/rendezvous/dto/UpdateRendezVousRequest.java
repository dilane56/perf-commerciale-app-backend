package com.cbcbourse.backend.rendezvous.dto;

import java.time.LocalDate;
import java.util.Set;

import com.cbcbourse.backend.rendezvous.TypeRendezVous;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Modification d un rendez-vous commercial")
public record UpdateRendezVousRequest(
        @Schema(description = "Client ou prospect rencontre", example = "1")
        @NotNull(message = "Le client est obligatoire") Long clientId,

        @Schema(description = "Date du rendez-vous ; peut etre dans le futur pour une visite planifiee",
                example = "2026-10-05")
        @NotNull(message = "La date est obligatoire") LocalDate date,

        @Schema(example = "PROSPECTION")
        @NotNull(message = "Le type de rendez-vous est obligatoire") TypeRendezVous type,

        @Schema(description = "Compte rendu remis au responsable") String compteRendu,

        @Schema(description = "Participants presents : au moins un, plusieurs en cas de binome",
                example = "[2, 3]")
        @NotEmpty(message = "Un rendez-vous doit compter au moins un participant")
        @Size(max = 20, message = "Trop de participants pour un seul rendez-vous") Set<Long> participantIds
) {
}
