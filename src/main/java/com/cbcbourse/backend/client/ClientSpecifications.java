package com.cbcbourse.backend.client;

import org.springframework.data.jpa.domain.Specification;

public final class ClientSpecifications {

    private ClientSpecifications() {
    }

    public static Specification<Client> hasReferent(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("commercialReferent").get("id"), userId);
    }

    public static Specification<Client> hasStatut(StatutClient statut) {
        return (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    public static Specification<Client> hasType(TypeClient type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /** Recherche libre sur le nom d'une personne physique comme sur la raison sociale d'une morale. */
    public static Specification<Client> designationContient(String fragment) {
        String motif = "%" + fragment.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("nom"), "")), motif),
                cb.like(cb.lower(cb.coalesce(root.get("raisonSociale"), "")), motif));
    }
}
