package com.cbcbourse.backend.rendezvous.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.cbcbourse.backend.rendezvous.RendezVous;
import com.cbcbourse.backend.rendezvous.TypeRendezVous;
import com.cbcbourse.backend.user.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rendez-vous commercial et ses participants")
public record RendezVousResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long clientId,
        @Schema(description = "Libelle du client rencontre", example = "SODIMA SA") String clientDesignation,
        @Schema(example = "2026-10-05") LocalDate date,
        @Schema(example = "PROSPECTION") TypeRendezVous type,
        String compteRendu,
        @Schema(description = "Personnes presentes au rendez-vous") List<ParticipantResponse> participants,
        Instant createdAt
) {
    @Schema(description = "Participant a un rendez-vous")
    public record ParticipantResponse(
            @Schema(example = "2") Long id,
            @Schema(example = "Awa Diallo") String nomComplet
    ) {
        static ParticipantResponse from(User user) {
            return new ParticipantResponse(user.getId(), user.getFirstName() + " " + user.getLastName());
        }
    }

    public static RendezVousResponse from(RendezVous rendezVous) {
        List<ParticipantResponse> participants = rendezVous.getParticipants().stream()
                .map(ParticipantResponse::from)
                .sorted(Comparator.comparing(ParticipantResponse::nomComplet))
                .toList();
        return new RendezVousResponse(
                rendezVous.getId(),
                rendezVous.getClient().getId(),
                rendezVous.getClient().designation(),
                rendezVous.getDate(),
                rendezVous.getType(),
                rendezVous.getCompteRendu(),
                participants,
                rendezVous.getCreatedAt()
        );
    }
}
