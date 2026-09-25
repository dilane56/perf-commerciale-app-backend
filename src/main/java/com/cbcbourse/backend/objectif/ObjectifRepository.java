package com.cbcbourse.backend.objectif;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ObjectifRepository extends JpaRepository<Objectif, Long>, JpaSpecificationExecutor<Objectif> {
}
