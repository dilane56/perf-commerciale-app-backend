package com.cbcbourse.backend.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import com.cbcbourse.backend.client.Client;
import com.cbcbourse.backend.client.StatutClient;
import com.cbcbourse.backend.client.TypeClient;
import com.cbcbourse.backend.kpi.dto.KpiCommercialResponse;
import com.cbcbourse.backend.kpi.dto.KpiPeriodeResponse;
import com.cbcbourse.backend.permission.Permission;
import com.cbcbourse.backend.rendezvous.RendezVous;
import com.cbcbourse.backend.rendezvous.TypeRendezVous;
import com.cbcbourse.backend.role.Role;
import com.cbcbourse.backend.transaction.SourceSysteme;
import com.cbcbourse.backend.transaction.Transaction;
import com.cbcbourse.backend.transaction.TypeTransaction;
import com.cbcbourse.backend.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie les regles d'attribution des KPI sur une base en memoire. Ce sont ces regles qui decident
 * a qui revient le credit d'un client : une erreur ici fausserait silencieusement l'evaluation des
 * commerciaux, sans provoquer la moindre exception.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KpiServiceTest {

    private static final LocalDate DEBUT_2026 = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN_2026 = LocalDate.of(2026, 12, 31);

    @Autowired
    private EntityManager em;

    @Autowired
    private KpiService kpiService;

    private User awa;
    private User moussa;
    private User fatou;

    @BeforeEach
    void seedJeuDeDonnees() {
        Permission viewOwn = persistPermission("VIEW_OWN_DASHBOARD");
        Permission viewTeam = persistPermission("VIEW_TEAM_DASHBOARD");

        Role commercial = persistRole("COMMERCIAL", Set.of(viewOwn));
        Role manager = persistRole("MANAGER_COMMERCIAL", Set.of(viewTeam));

        awa = persistUser("Awa", "Diallo", "awa@test.local", Set.of(commercial));
        moussa = persistUser("Moussa", "Kone", "moussa@test.local", Set.of(commercial));
        // Le responsable accompagne les commerciaux sans etre referent d'aucun client.
        fatou = persistUser("Fatou", "Sow", "fatou@test.local", Set.of(manager));

        Client clientAwa = persistClient("Ibrahim Traore", awa, LocalDate.of(2026, 1, 15));
        Client clientAwaAncien = persistClient("Aminata Barry", awa, LocalDate.of(2025, 12, 1));
        Client clientMoussa = persistClient("Jean Ouedraogo", moussa, LocalDate.of(2026, 3, 1));

        persistTransaction(clientAwa, TypeTransaction.INTERMEDIATION, "1000", LocalDate.of(2026, 2, 1));
        persistTransaction(clientAwa, TypeTransaction.GESTION_SOUS_MANDAT, "5000", LocalDate.of(2026, 3, 1));
        persistTransaction(clientAwaAncien, TypeTransaction.INTERMEDIATION, "999", LocalDate.of(2025, 6, 1));
        persistTransaction(clientMoussa, TypeTransaction.INTERMEDIATION, "2000", LocalDate.of(2026, 4, 1));

        persistRendezVous(clientAwa, LocalDate.of(2026, 1, 10), Set.of(awa, fatou));
        persistRendezVous(clientMoussa, LocalDate.of(2026, 3, 5), Set.of(moussa, fatou, awa));
        persistRendezVous(clientAwa, LocalDate.of(2025, 5, 5), Set.of(awa));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Le referent recoit le credit de ses clients et des montants collectes sur la periode")
    void kpisDuReferent() {
        KpiCommercialResponse kpi = kpiService.forUser(awa.getId(), DEBUT_2026, FIN_2026);

        assertThat(kpi.nouveauxClients()).isEqualTo(1); // le client acquis en 2025 est exclu
        assertThat(kpi.montantCollecte()).isEqualByComparingTo("6000"); // la transaction de 2025 est exclue
        assertThat(kpi.mandatsSignes()).isEqualTo(1);
        assertThat(kpi.rendezVous()).isEqualTo(2); // le rendez-vous de 2025 est exclu
    }

    @Test
    @DisplayName("Accompagner des rendez-vous ne donne aucun client ni aucun montant au responsable")
    void participationNeDonnePasDeCredit() {
        KpiCommercialResponse kpi = kpiService.forUser(fatou.getId(), DEBUT_2026, FIN_2026);

        assertThat(kpi.nouveauxClients()).isZero();
        assertThat(kpi.montantCollecte()).isEqualByComparingTo("0");
        assertThat(kpi.mandatsSignes()).isZero();
        // Son effort terrain reste visible : c'est tout l'interet de separer les deux notions.
        assertThat(kpi.rendezVous()).isEqualTo(2);
    }

    @Test
    @DisplayName("Assister au rendez-vous d'un collegue n'attribue pas son client")
    void binomeNAttribuePasLeClientDuCollegue() {
        KpiCommercialResponse kpiMoussa = kpiService.forUser(moussa.getId(), DEBUT_2026, FIN_2026);

        assertThat(kpiMoussa.nouveauxClients()).isEqualTo(1);
        assertThat(kpiMoussa.montantCollecte()).isEqualByComparingTo("2000");
        assertThat(kpiMoussa.rendezVous()).isEqualTo(1);

        // Awa etait presente au rendez-vous du client de Moussa : cela compte dans son activite...
        KpiCommercialResponse kpiAwa = kpiService.forUser(awa.getId(), DEBUT_2026, FIN_2026);
        assertThat(kpiAwa.rendezVous()).isEqualTo(2);
        // ...mais pas dans ses montants collectes.
        assertThat(kpiAwa.montantCollecte()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("La vue consolidee couvre les commerciaux suivis et ne double compte aucun montant")
    void vueConsolidee() {
        KpiPeriodeResponse consolide = kpiService.forAllCommerciaux(DEBUT_2026, FIN_2026);

        assertThat(consolide.parCommercial()).hasSize(2);
        assertThat(consolide.parCommercial())
                .extracting(KpiCommercialResponse::email)
                .containsExactlyInAnyOrder("awa@test.local", "moussa@test.local");
        assertThat(consolide.totalNouveauxClients()).isEqualTo(2);
        assertThat(consolide.totalMontantCollecte()).isEqualByComparingTo("8000");
        assertThat(consolide.totalMandatsSignes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Une periode sans activite renvoie des indicateurs a zero plutot qu'une erreur")
    void periodeVide() {
        KpiCommercialResponse kpi = kpiService.forUser(awa.getId(),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(kpi.nouveauxClients()).isZero();
        assertThat(kpi.montantCollecte()).isEqualByComparingTo("0");
        assertThat(kpi.rendezVous()).isZero();
    }

    private Permission persistPermission(String code) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setDescription(code);
        em.persist(permission);
        return permission;
    }

    private Role persistRole(String name, Set<Permission> permissions) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(name);
        role.setPermissions(new java.util.HashSet<>(permissions));
        em.persist(role);
        return role;
    }

    private User persistUser(String firstName, String lastName, String email, Set<Role> roles) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash("hash-de-test");
        user.setActive(true);
        user.setRoles(new java.util.HashSet<>(roles));
        em.persist(user);
        return user;
    }

    private Client persistClient(String nom, User referent, LocalDate dateAcquisition) {
        Client client = new Client();
        client.setType(TypeClient.PERSONNE_PHYSIQUE);
        client.setNom(nom);
        client.setCommercialReferent(referent);
        client.setDateAcquisition(dateAcquisition);
        client.setStatut(StatutClient.CLIENT_ACTIF);
        em.persist(client);
        return client;
    }

    private void persistTransaction(Client client, TypeTransaction type, String montant, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setType(type);
        transaction.setMontant(new BigDecimal(montant));
        transaction.setDateTransaction(date);
        transaction.setSourceSysteme(SourceSysteme.MANUEL);
        em.persist(transaction);
    }

    private void persistRendezVous(Client client, LocalDate date, Set<User> participants) {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setClient(client);
        rendezVous.setDate(date);
        rendezVous.setType(TypeRendezVous.PROSPECTION);
        rendezVous.setCompteRendu("Compte rendu de test");
        rendezVous.setParticipants(new java.util.HashSet<>(participants));
        em.persist(rendezVous);
    }
}
