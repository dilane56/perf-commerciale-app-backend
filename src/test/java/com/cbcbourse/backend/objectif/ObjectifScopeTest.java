package com.cbcbourse.backend.objectif;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cbcbourse.backend.auth.dto.AuthenticatedUser;
import com.cbcbourse.backend.client.Client;
import com.cbcbourse.backend.client.StatutClient;
import com.cbcbourse.backend.client.TypeClient;
import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.security.Permissions;
import com.cbcbourse.backend.objectif.dto.CreateObjectifRequest;
import com.cbcbourse.backend.objectif.dto.ObjectifResponse;
import com.cbcbourse.backend.objectif.dto.ObjectifsEquipeResponse;
import com.cbcbourse.backend.objectif.dto.UpdateObjectifRequest;
import com.cbcbourse.backend.user.User;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifie le cloisonnement des objectifs : un responsable ne fixe des objectifs individuels ou
 * d'equipe que pour sa propre equipe, MANAGE_ALL_OBJECTIVES seul etend ce perimetre. Une regression
 * ici laisserait un responsable fixer ou reaffecter les objectifs d'une autre equipe sans erreur.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ObjectifScopeTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectifService objectifService;

    private User fatou;
    private User pierre;
    private User awa;
    private User moussa;
    private User diane;

    @BeforeEach
    void seed() {
        fatou = persistUser("Fatou", "Sow", "fatou@test.local", null);
        pierre = persistUser("Pierre", "Ba", "pierre@test.local", null);
        awa = persistUser("Awa", "Diallo", "awa@test.local", fatou);
        moussa = persistUser("Moussa", "Kone", "moussa@test.local", fatou);
        diane = persistUser("Diane", "Toure", "diane@test.local", null);

        persistClient("Client Awa 1", awa);
        persistClient("Client Awa 2", awa);
        persistClient("Client Moussa 1", moussa);

        em.flush();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un responsable fixe un objectif individuel pour un membre de son equipe")
    void objectifIndividuelPourSaPropreEquipe() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);

        ObjectifResponse cree = objectifService.create(
                new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, awa.getId(), null,
                        new BigDecimal("2"), DEBUT, FIN));

        assertThat(cree.valeurReelle()).isEqualByComparingTo("2");
        assertThat(cree.tauxAtteinte()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("Un responsable ne peut pas fixer un objectif pour un commercial sans manager")
    void objectifIndividuelPourCommercialSansManagerRefuse() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);

        CreateObjectifRequest request = new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS,
                diane.getId(), null, new BigDecimal("1"), DEBUT, FIN);

        assertThatThrownBy(() -> objectifService.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un responsable fixe un objectif pour sa propre equipe, agregeant ses membres")
    void objectifEquipePourSaPropreEquipe() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);

        ObjectifResponse cree = objectifService.create(
                new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, null, fatou.getId(),
                        new BigDecimal("3"), DEBUT, FIN));

        assertThat(cree.valeurReelle()).isEqualByComparingTo("3");
        assertThat(cree.tauxAtteinte()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("Un responsable ne peut pas fixer un objectif pour l'equipe d'un autre responsable")
    void objectifEquipeDunAutreResponsableRefuse() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);

        CreateObjectifRequest request = new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, null,
                pierre.getId(), new BigDecimal("1"), DEBUT, FIN);

        assertThatThrownBy(() -> objectifService.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("MANAGE_ALL_OBJECTIVES permet de fixer un objectif pour n'importe qui")
    void manageAllObjectivesEtendLePerimetre() {
        authentifier(fatou, Permissions.MANAGE_ALL_OBJECTIVES);

        ObjectifResponse cree = objectifService.create(
                new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, diane.getId(), null,
                        new BigDecimal("1"), DEBUT, FIN));

        assertThat(cree.commercialId()).isEqualTo(diane.getId());
    }

    @Test
    @DisplayName("Un objectif doit viser exactement un titulaire")
    void titulaireIncoherentRefuse() {
        authentifier(fatou, Permissions.MANAGE_ALL_OBJECTIVES);

        CreateObjectifRequest ni_lun_ni_lautre = new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS,
                null, null, new BigDecimal("1"), DEBUT, FIN);
        CreateObjectifRequest les_deux = new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS,
                awa.getId(), fatou.getId(), new BigDecimal("1"), DEBUT, FIN);

        assertThatThrownBy(() -> objectifService.create(ni_lun_ni_lautre))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> objectifService.create(les_deux))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("La date de fin ne peut pas preceder la date de debut")
    void periodeIncoherenteRefusee() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);

        CreateObjectifRequest request = new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS,
                awa.getId(), null, new BigDecimal("1"), FIN, DEBUT);

        assertThatThrownBy(() -> objectifService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
    }

    @Test
    @DisplayName("Modifier un objectif ne permet pas de s'approprier celui d'une autre equipe")
    void reaffectationHorsPerimetreRefusee() {
        authentifier(pierre, Permissions.MANAGE_OBJECTIVES);
        ObjectifResponse objectifDePierre = objectifService.create(
                new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, null, pierre.getId(),
                        new BigDecimal("1"), DEBUT, FIN));

        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);
        UpdateObjectifRequest reaffectation = new UpdateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS,
                null, fatou.getId(), new BigDecimal("1"), DEBUT, FIN);

        assertThatThrownBy(() -> objectifService.update(objectifDePierre.id(), reaffectation))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("La vue equipe regroupe l'objectif d'equipe et les objectifs individuels des membres")
    void vueEquipeRegroupeLesObjectifs() {
        authentifier(fatou, Permissions.MANAGE_OBJECTIVES);
        objectifService.create(new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, null,
                fatou.getId(), new BigDecimal("3"), DEBUT, FIN));
        objectifService.create(new CreateObjectifRequest(TypeObjectif.NOUVEAUX_CLIENTS, awa.getId(),
                null, new BigDecimal("2"), DEBUT, FIN));

        ObjectifsEquipeResponse vueEquipe = objectifService.objectifsEquipe(fatou.getId());

        assertThat(vueEquipe.objectifsEquipe()).hasSize(1);
        assertThat(vueEquipe.objectifsIndividuels()).hasSize(1);
        assertThat(vueEquipe.objectifsIndividuels().get(0).commercialId()).isEqualTo(awa.getId());
    }

    private void authentifier(User user, String... authorities) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getEmail()),
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User persistUser(String firstName, String lastName, String email, User manager) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash("hash-de-test");
        user.setActive(true);
        user.setManager(manager);
        em.persist(user);
        return user;
    }

    private void persistClient(String nom, User referent) {
        Client client = new Client();
        client.setType(TypeClient.PERSONNE_PHYSIQUE);
        client.setNom(nom);
        client.setCommercialReferent(referent);
        client.setDateAcquisition(LocalDate.of(2026, 2, 1));
        client.setStatut(StatutClient.CLIENT_ACTIF);
        em.persist(client);
    }
}
