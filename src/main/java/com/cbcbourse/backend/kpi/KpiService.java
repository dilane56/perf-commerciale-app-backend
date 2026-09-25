package com.cbcbourse.backend.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cbcbourse.backend.client.ClientRepository;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.kpi.dto.KpiCommercialResponse;
import com.cbcbourse.backend.kpi.dto.KpiPeriodeResponse;
import com.cbcbourse.backend.rendezvous.RendezVousRepository;
import com.cbcbourse.backend.transaction.TransactionRepository;
import com.cbcbourse.backend.transaction.TypeTransaction;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcule les indicateurs de performance a la demande a partir des evenements bruts (clients,
 * rendez-vous, transactions). Rien n'est stocke : si les regles d'attribution evoluent, seul ce
 * service change, sans migration ni recalcul d'historique.
 */
@Service
@Transactional(readOnly = true)
public class KpiService {

    /**
     * Permission qui designe les utilisateurs dont la performance individuelle est suivie. On part de
     * la permission et non d'un nom de role, pour que l'administrateur puisse reorganiser les roles
     * depuis l'interface sans modifier le code.
     */
    private static final String PERMISSION_SUIVI_INDIVIDUEL = "VIEW_OWN_DASHBOARD";

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final RendezVousRepository rendezVousRepository;
    private final UserRepository userRepository;

    public KpiService(ClientRepository clientRepository, TransactionRepository transactionRepository,
                      RendezVousRepository rendezVousRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.userRepository = userRepository;
    }

    /** KPI d'un commercial identifie par son id. */
    public KpiCommercialResponse forUser(Long userId, LocalDate debut, LocalDate fin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + userId));
        return compute(user, debut, fin);
    }

    /** Vue managériale : un bloc de KPI par commercial suivi, plus les totaux de l'equipe. */
    public KpiPeriodeResponse forAllCommerciaux(LocalDate debut, LocalDate fin) {
        List<KpiCommercialResponse> parCommercial = userRepository
                .findActiveByPermissionCode(PERMISSION_SUIVI_INDIVIDUEL).stream()
                .map(user -> compute(user, debut, fin))
                .toList();
        return KpiPeriodeResponse.of(debut, fin, parCommercial);
    }

    /**
     * Vue d'equipe : un bloc de KPI par membre actif de l'equipe du responsable designe, plus les
     * totaux de cette equipe. Contrairement a {@link #forAllCommerciaux}, le perimetre se deduit du
     * lien manager -> commerciaux plutot que d'une permission : c'est l'appartenance a l'equipe qui
     * compte ici, pas le suivi individuel a l'echelle de l'entreprise.
     */
    public KpiPeriodeResponse forTeam(Long managerId, LocalDate debut, LocalDate fin) {
        List<KpiCommercialResponse> parCommercial = userRepository
                .findByManagerIdAndActiveTrue(managerId).stream()
                .map(user -> compute(user, debut, fin))
                .toList();
        return KpiPeriodeResponse.of(debut, fin, parCommercial);
    }

    private KpiCommercialResponse compute(User user, LocalDate debut, LocalDate fin) {
        Long userId = user.getId();

        long nouveauxClients = clientRepository
                .countByCommercialReferentIdAndDateAcquisitionBetween(userId, debut, fin);
        BigDecimal montantCollecte = transactionRepository.sumMontantByCommercial(userId, debut, fin);
        long mandatsSignes = transactionRepository
                .countByCommercialAndType(userId, TypeTransaction.GESTION_SOUS_MANDAT, debut, fin);
        // Volontairement base sur la participation et non sur le referent : c'est la seule mesure qui
        // reflete l'effort du responsable accompagnant un commercial sur le terrain.
        long rendezVous = rendezVousRepository.countParticipationsByUser(userId, debut, fin);

        return new KpiCommercialResponse(
                userId,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                nouveauxClients,
                montantCollecte == null ? BigDecimal.ZERO : montantCollecte,
                mandatsSignes,
                rendezVous
        );
    }
}
