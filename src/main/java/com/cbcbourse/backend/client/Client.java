package com.cbcbourse.backend.client;

import java.time.Instant;
import java.time.LocalDate;

import com.cbcbourse.backend.user.User;

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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Client ou prospect suivi par le service commercial.
 *
 * <p>Le commercial referent est volontairement une relation <b>unique</b> : c'est lui qui recoit le
 * credit du client dans les KPI individuels (nouveaux clients, montants collectes). La presence
 * d'autres collaborateurs lors des rendez-vous est tracee separement, via
 * {@code rendez_vous_participants}, pour mesurer l'effort terrain sans fausser cette attribution.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeClient type;

    /** Nom et prenom pour une personne physique, laisse vide pour une personne morale. */
    @Column(length = 255)
    private String nom;

    /** Denomination sociale pour une personne morale, laissee vide pour une personne physique. */
    @Column(name = "raison_sociale", length = 255)
    private String raisonSociale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commercial_referent_id", nullable = false)
    private User commercialReferent;

    /** Date d'entree en relation : sert au KPI "nouveaux clients" sur une periode donnee. */
    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutClient statut = StatutClient.PROSPECT;

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Libelle d'affichage, quel que soit le type de client. */
    public String designation() {
        return type == TypeClient.PERSONNE_MORALE ? raisonSociale : nom;
    }
}
