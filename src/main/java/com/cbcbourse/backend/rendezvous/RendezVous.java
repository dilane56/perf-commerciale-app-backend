package com.cbcbourse.backend.rendezvous;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.cbcbourse.backend.client.Client;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Rendez-vous commercial tenu avec un client ou un prospect.
 *
 * <p>Les commerciaux prospectent souvent en binome, parfois accompagnes du responsable : les
 * participants sont donc une relation many-to-many et jamais une simple cle etrangere vers un seul
 * utilisateur. Cette table mesure l'activite terrain, pas la propriete du client.
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRendezVous type;

    /** Compte rendu libre remis au responsable, stocke en NVARCHAR(MAX) cote SQL Server. */
    @JdbcTypeCode(SqlTypes.LONGNVARCHAR)
    @Column(name = "compte_rendu")
    private String compteRendu;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rendez_vous_participants",
            joinColumns = @JoinColumn(name = "rendez_vous_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
