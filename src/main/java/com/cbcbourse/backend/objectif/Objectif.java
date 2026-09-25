package com.cbcbourse.backend.objectif;

import java.math.BigDecimal;
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
 * Objectif fixe sur une periode, soit a un commercial (objectif individuel), soit a l'equipe d'un
 * responsable (objectif d'equipe) : exactement un des deux titulaires est renseigne.
 *
 * <p>Un objectif d'equipe est une valeur propre que le responsable peut fixer, pas mecaniquement la
 * somme des objectifs individuels de ses commerciaux : les deux existent independamment l'un de
 * l'autre. La valeur reelle et le taux d'atteinte ne sont jamais stockes ici : ils sont recalcules a
 * la demande via le meme {@code KpiService} que les tableaux de bord.
 */
@Entity
@Table(name = "objectifs")
@Getter
@Setter
@NoArgsConstructor
public class Objectif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeObjectif type;

    /** Renseigne pour un objectif individuel, nul pour un objectif d'equipe. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_id")
    private User commercial;

    /** Renseigne pour un objectif d'equipe : le responsable dont l'equipe est visee. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "valeur_cible", nullable = false, precision = 18, scale = 2)
    private BigDecimal valeurCible;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Auteurs de la saisie, renseignes a partir de l'utilisateur authentifie. */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
