package com.cbcbourse.backend.rendezvous;

import java.time.LocalDate;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class RendezVousSpecifications {

    private RendezVousSpecifications() {
    }

    /** Rendez-vous des clients dont l'utilisateur donne est le referent. */
    public static Specification<RendezVous> clientDuReferent(Long userId) {
        return (root, query, cb) -> cb.equal(
                root.get("client").get("commercialReferent").get("id"), userId);
    }

    public static Specification<RendezVous> pourClient(Long clientId) {
        return (root, query, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    /** Rendez-vous auxquels l'utilisateur a assiste, qu'il soit referent du client ou simple accompagnant. */
    public static Specification<RendezVous> avecParticipant(Long userId) {
        return (root, query, cb) -> {
            if (query != null) {
                // La jointure sur une collection multiplie les lignes du rendez-vous.
                query.distinct(true);
            }
            return cb.equal(root.join("participants", JoinType.INNER).get("id"), userId);
        };
    }

    public static Specification<RendezVous> aPartirDu(LocalDate debut) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), debut);
    }

    public static Specification<RendezVous> jusquAu(LocalDate fin) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), fin);
    }
}
