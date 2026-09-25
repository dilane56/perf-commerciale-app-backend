package com.cbcbourse.backend.rendezvous;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long>,
        JpaSpecificationExecutor<RendezVous> {

    /**
     * Activite terrain : compte les rendez-vous auxquels l'utilisateur a <b>participe</b>, qu'il soit
     * ou non le referent du client. Valorise l'effort, y compris sans signature a la cle.
     */
    @Query("""
            select count(r) from RendezVous r
            join r.participants p
            where p.id = :userId and r.date between :debut and :fin
            """)
    long countParticipationsByUser(@Param("userId") Long userId, @Param("debut") LocalDate debut,
                                    @Param("fin") LocalDate fin);
}
