package com.cbcbourse.backend.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.cbcbourse.backend.auth.dto.AuthenticatedUser;
import com.cbcbourse.backend.client.dto.CreateClientRequest;
import com.cbcbourse.backend.client.dto.UpdateClientRequest;
import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.security.Permissions;
import com.cbcbourse.backend.permission.Permission;
import com.cbcbourse.backend.rendezvous.RendezVousService;
import com.cbcbourse.backend.rendezvous.TypeRendezVous;
import com.cbcbourse.backend.rendezvous.dto.CreateRendezVousRequest;
import com.cbcbourse.backend.role.Role;
import com.cbcbourse.backend.transaction.SourceSysteme;
import com.cbcbourse.backend.transaction.Transaction;
import com.cbcbourse.backend.transaction.TypeTransaction;
import com.cbcbourse.backend.user.User;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifie le cloisonnement du portefeuille : un commercial ne travaille que sur ses propres clients.
 * La permission dit ce qu'on a le droit de faire, ces regles disent sur quelles lignes — une
 * regression ici laisserait un commercial modifier le portefeuille d'un collegue sans aucune erreur
 * visible.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PortfolioScopeTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RendezVousService rendezVousService;

    private User awa;
    private User moussa;
    private User fatou;
    private Client clientDAwa;
    private Client clientDeMoussa;

    @BeforeEach
    void seed() {
        Permission gererSien = persistPermission(Permissions.MANAGE_OWN_PORTFOLIO);
        Permission gererTous = persistPermission(Permissions.MANAGE_ALL_PORTFOLIOS);

        Role commercial = persistRole("COMMERCIAL", Set.of(gererSien));
        Role manager = persistRole("MANAGER_COMMERCIAL", Set.of(gererTous));

        awa = persistUser("Awa", "Diallo", "awa@test.local", Set.of(commercial));
        moussa = persistUser("Moussa", "Kone", "moussa@test.local", Set.of(commercial));
        fatou = persistUser("Fatou", "Sow", "fatou@test.local", Set.of(manager));

        clientDAwa = persistClient("Ibrahim Traore", awa);
        clientDeMoussa = persistClient("Jean Ouedraogo", moussa);

        em.flush();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un commercial ne voit que les clients dont il est le referent")
    void listeRestreinteAuPerimetre() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        var page = clientService.search(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Client::getId).containsExactly(clientDAwa.getId());
    }

    @Test
    @DisplayName("Un filtre explicite ne permet pas d'elargir son perimetre")
    void filtreNElargitPasLePerimetre() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        // Awa demande explicitement le portefeuille de Moussa : la restriction doit primer.
        var page = clientService.search(moussa.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Un commercial ne peut pas consulter le client d'un collegue")
    void lectureHorsPerimetreRefusee() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        assertThatThrownBy(() -> clientService.findById(clientDeMoussa.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un commercial ne peut pas creer un client pour un collegue")
    void creationPourUnCollegueRefusee() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        CreateClientRequest request = new CreateClientRequest(TypeClient.PERSONNE_PHYSIQUE, "Nouveau Prospect",
                null, moussa.getId(), LocalDate.now(), StatutClient.PROSPECT);

        assertThatThrownBy(() -> clientService.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Le referent est force au commercial connecte et l'auteur de la saisie est trace")
    void creationForceLeReferentEtTraceLAuteur() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        Client cree = clientService.create(new CreateClientRequest(TypeClient.PERSONNE_MORALE, null,
                "SODIMA SA", null, LocalDate.now(), StatutClient.PROSPECT));

        assertThat(cree.getCommercialReferent().getId()).isEqualTo(awa.getId());
        assertThat(cree.getCreatedBy()).isEqualTo(awa.getId());
        assertThat(cree.getNom()).isNull();
        assertThat(cree.designation()).isEqualTo("SODIMA SA");
    }

    @Test
    @DisplayName("Un responsable voit tout le portefeuille et peut reaffecter un client")
    void responsableGereToutLePortefeuille() {
        authentifier(fatou, Permissions.MANAGE_ALL_PORTFOLIOS);

        var page = clientService.search(null, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(2);

        Client reaffecte = clientService.update(clientDeMoussa.getId(),
                new UpdateClientRequest(TypeClient.PERSONNE_PHYSIQUE, "Jean Ouedraogo", null, awa.getId(),
                        LocalDate.of(2026, 3, 1), StatutClient.CLIENT_ACTIF));

        assertThat(reaffecte.getCommercialReferent().getId()).isEqualTo(awa.getId());
    }

    @Test
    @DisplayName("Une personne morale sans raison sociale est refusee avec un message lisible")
    void designationIncoherenteRefusee() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        CreateClientRequest request = new CreateClientRequest(TypeClient.PERSONNE_MORALE, "Un nom", null,
                null, LocalDate.now(), StatutClient.PROSPECT);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("raison sociale");
    }

    @Test
    @DisplayName("Une date d'acquisition dans le futur est refusee")
    void dateAcquisitionFutureRefusee() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        CreateClientRequest request = new CreateClientRequest(TypeClient.PERSONNE_PHYSIQUE, "Futur Client", null,
                null, LocalDate.now().plusDays(1), StatutClient.PROSPECT);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("futur");
    }

    @Test
    @DisplayName("Un client portant des transactions Atlantis ne peut pas etre supprime")
    void suppressionRefuseeSiTransactions() {
        persistTransaction(clientDAwa);
        em.flush();

        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        assertThatThrownBy(() -> clientService.delete(clientDAwa.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLIENT_INACTIF");
    }

    @Test
    @DisplayName("Un commercial ne peut pas programmer un rendez-vous sur le client d'un collegue")
    void rendezVousHorsPerimetreRefuse() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        CreateRendezVousRequest request = new CreateRendezVousRequest(clientDeMoussa.getId(),
                LocalDate.now().plusDays(3), TypeRendezVous.PROSPECTION, "Visite", Set.of(awa.getId()));

        assertThatThrownBy(() -> rendezVousService.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un rendez-vous peut etre planifie dans le futur et accepte plusieurs participants")
    void rendezVousPlanifieEnBinome() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        var cree = rendezVousService.create(new CreateRendezVousRequest(clientDAwa.getId(),
                LocalDate.now().plusDays(10), TypeRendezVous.PROSPECTION, "Prospection en binome",
                Set.of(awa.getId(), fatou.getId())));

        assertThat(cree.getParticipants()).extracting(User::getId)
                .containsExactlyInAnyOrder(awa.getId(), fatou.getId());
        assertThat(cree.getCreatedBy()).isEqualTo(awa.getId());
    }

    @Test
    @DisplayName("Un rendez-vous sans participant est refuse : son activite ne serait imputee a personne")
    void rendezVousSansParticipantRefuse() {
        authentifier(awa, Permissions.MANAGE_OWN_PORTFOLIO);

        CreateRendezVousRequest request = new CreateRendezVousRequest(clientDAwa.getId(), LocalDate.now(),
                TypeRendezVous.SUIVI, null, Set.of());

        assertThatThrownBy(() -> rendezVousService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("participant");
    }

    private void authentifier(User user, String... authorities) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getEmail()),
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
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

    private Client persistClient(String nom, User referent) {
        Client client = new Client();
        client.setType(TypeClient.PERSONNE_PHYSIQUE);
        client.setNom(nom);
        client.setCommercialReferent(referent);
        client.setDateAcquisition(LocalDate.of(2026, 1, 15));
        client.setStatut(StatutClient.CLIENT_ACTIF);
        em.persist(client);
        return client;
    }

    private void persistTransaction(Client client) {
        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setType(TypeTransaction.INTERMEDIATION);
        transaction.setMontant(new BigDecimal("1000"));
        transaction.setDateTransaction(LocalDate.of(2026, 2, 1));
        transaction.setSourceSysteme(SourceSysteme.ATLANTIS);
        em.persist(transaction);
    }
}
