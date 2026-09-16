package com.cbcbourse.backend.user;

import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRoleName(String roleName) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("roles").get("name"), roleName);
        };
    }

    public static Specification<User> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
