package com.cbcbourse.backend.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    /**
     * Nouveaux clients d'un commercial sur une periode : seul le referent est compte, afin que
     * l'accompagnement d'un collegue en rendez-vous ne cree pas de double comptage.
     */
    long countByCommercialReferentIdAndDateAcquisitionBetween(Long commercialReferentId, LocalDate debut,
                                                              LocalDate fin);

    List<Client> findByCommercialReferentId(Long commercialReferentId);
}
