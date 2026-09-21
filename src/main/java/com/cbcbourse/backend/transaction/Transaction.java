package com.cbcbourse.backend.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cbcbourse.backend.client.Client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evenement financier rattache a un client : collecte d'actifs, signature de mandat ou commission
 * facturee. C'est la matiere premiere des KPI de chiffre d'affaires et d'encours.
 *
 * <p>Le montant est un {@link BigDecimal} adosse a une colonne {@code DECIMAL(18,2)} : les types
 * flottants sont exclus pour des montants financiers. La devise est implicitement le XOF/FCFA tant
 * que le multi-devise n'est pas demande.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeTransaction type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_transaction", nullable = false)
    private LocalDate dateTransaction;

    @Column(length = 500)
    private String description;

    /**
     * Origine de la donnee. Un {@code sourceReferenceId} (identifiant de la transaction dans Atlantis)
     * pourra etre ajoute par une migration ulterieure sans remettre en cause ce modele.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_systeme", nullable = false, length = 30)
    private SourceSysteme sourceSysteme = SourceSysteme.MANUEL;

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
