package com.cbcbourse.backend.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Montants collectes par un commercial : les transactions sont rattachees au client, et le client
     * a un referent unique. Le credit suit donc toujours le referent.
     */
    @Query("""
            select coalesce(sum(t.montant), 0) from Transaction t
            where t.client.commercialReferent.id = :userId
              and t.dateTransaction between :debut and :fin
            """)
    BigDecimal sumMontantByCommercial(@Param("userId") Long userId, @Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin);

    @Query("""
            select count(t) from Transaction t
            where t.client.commercialReferent.id = :userId
              and t.type = :type
              and t.dateTransaction between :debut and :fin
            """)
    long countByCommercialAndType(@Param("userId") Long userId, @Param("type") TypeTransaction type,
                                   @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
