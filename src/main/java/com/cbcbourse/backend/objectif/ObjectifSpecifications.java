package com.cbcbourse.backend.objectif;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public final class ObjectifSpecifications {

    private ObjectifSpecifications() {
    }

    public static Specification<Objectif> pourCommercial(Long commercialId) {
        return (root, query, cb) -> cb.equal(root.get("commercial").get("id"), commercialId);
    }

    public static Specification<Objectif> pourCommerciaux(List<Long> commercialIds) {
        return (root, query, cb) -> root.get("commercial").get("id").in(commercialIds);
    }

    public static Specification<Objectif> pourManager(Long managerId) {
        return (root, query, cb) -> cb.equal(root.get("manager").get("id"), managerId);
    }

    public static Specification<Objectif> deType(TypeObjectif type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }
}
