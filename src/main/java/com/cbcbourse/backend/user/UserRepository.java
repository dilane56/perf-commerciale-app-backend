package com.cbcbourse.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    /**
     * Utilisateurs actifs dont la performance individuelle est suivie, identifies par la permission
     * qu'ils detiennent plutot que par un nom de role code en dur : l'administrateur reste libre de
     * reorganiser les roles sans toucher au code.
     */
    @Query("""
            select distinct u from User u
            join u.roles r
            join r.permissions p
            where u.active = true and p.code = :permissionCode
            order by u.lastName, u.firstName
            """)
    List<User> findActiveByPermissionCode(@Param("permissionCode") String permissionCode);

    boolean existsByEmail(String email);

    boolean existsByRoles_Id(Long roleId);
}
