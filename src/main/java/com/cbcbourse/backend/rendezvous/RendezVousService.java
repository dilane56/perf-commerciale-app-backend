package com.cbcbourse.backend.rendezvous;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cbcbourse.backend.client.Client;
import com.cbcbourse.backend.client.ClientRepository;
import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.common.security.PortfolioAccess;
import com.cbcbourse.backend.rendezvous.dto.CreateRendezVousRequest;
import com.cbcbourse.backend.rendezvous.dto.RendezVousResponse;
import com.cbcbourse.backend.rendezvous.dto.UpdateRendezVousRequest;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des rendez-vous commerciaux.
 *
 * <p>Le perimetre d'un rendez-vous decoule de celui de son client : un commercial gere les rendez-vous
 * des clients dont il est le referent. Les participants, eux, peuvent etre n'importe quels utilisateurs
 * actifs, puisque l'equipe prospecte souvent en binome et que le responsable accompagne ses commerciaux.
 */
@Service
@Transactional
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PortfolioAccess portfolioAccess;

    public RendezVousService(RendezVousRepository rendezVousRepository, ClientRepository clientRepository,
                             UserRepository userRepository, PortfolioAccess portfolioAccess) {
        this.rendezVousRepository = rendezVousRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.portfolioAccess = portfolioAccess;
    }

    @Transactional(readOnly = true)
    public Page<RendezVousResponse> search(Long clientId, Long participantId, LocalDate debut, LocalDate fin,
                                           Pageable pageable) {
        Specification<RendezVous> spec = Specification.unrestricted();

        if (!portfolioAccess.canManageAllPortfolios()) {
            // Restreint aux rendez-vous des clients du commercial connecte, quels que soient les filtres.
            spec = spec.and(RendezVousSpecifications.clientDuReferent(portfolioAccess.currentUserId()));
        }
        if (clientId != null) {
            spec = spec.and(RendezVousSpecifications.pourClient(clientId));
        }
        if (participantId != null) {
            spec = spec.and(RendezVousSpecifications.avecParticipant(participantId));
        }
        if (debut != null) {
            spec = spec.and(RendezVousSpecifications.aPartirDu(debut));
        }
        if (fin != null) {
            spec = spec.and(RendezVousSpecifications.jusquAu(fin));
        }

        // Construit le DTO ici, encore a l'interieur de la transaction : le client et les participants
        // sont charges paresseusement, et open-in-view etant desactive, y toucher depuis le controleur
        // leverait une LazyInitializationException une fois la session fermee.
        return rendezVousRepository.findAll(spec, pageable).map(RendezVousResponse::from);
    }

    @Transactional(readOnly = true)
    public RendezVousResponse findById(Long id) {
        return RendezVousResponse.from(getEntity(id));
    }

    public RendezVousResponse create(CreateRendezVousRequest request) {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setClient(loadClientDansPerimetre(request.clientId()));
        rendezVous.setDate(request.date());
        rendezVous.setType(request.type());
        rendezVous.setCompteRendu(request.compteRendu());
        rendezVous.setParticipants(loadParticipants(request.participantIds()));

        rendezVous.setCreatedBy(portfolioAccess.currentUserId());
        rendezVous.setUpdatedBy(portfolioAccess.currentUserId());
        return RendezVousResponse.from(rendezVousRepository.save(rendezVous));
    }

    public RendezVousResponse update(Long id, UpdateRendezVousRequest request) {
        RendezVous rendezVous = getEntity(id);

        // Deplacer un rendez-vous vers un autre client exige aussi d'avoir acces au client d'arrivee.
        rendezVous.setClient(loadClientDansPerimetre(request.clientId()));
        rendezVous.setDate(request.date());
        rendezVous.setType(request.type());
        rendezVous.setCompteRendu(request.compteRendu());
        rendezVous.setParticipants(loadParticipants(request.participantIds()));

        rendezVous.setUpdatedBy(portfolioAccess.currentUserId());
        return RendezVousResponse.from(rendezVousRepository.save(rendezVous));
    }

    public void delete(Long id) {
        rendezVousRepository.delete(getEntity(id));
    }

    /** Reservee a l'usage interne : renvoie l'entite, pas le DTO, pour que create/update puissent la muter. */
    private RendezVous getEntity(Long id) {
        RendezVous rendezVous = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable: " + id));
        portfolioAccess.checkCanActOnPortfolioOf(rendezVous.getClient().getCommercialReferent().getId());
        return rendezVous;
    }

    private Client loadClientDansPerimetre(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable: " + clientId));
        portfolioAccess.checkCanActOnPortfolioOf(client.getCommercialReferent().getId());
        return client;
    }

    /**
     * Sans participant, l'activite terrain du rendez-vous ne serait imputee a personne et n'apparaitrait
     * dans aucun KPI.
     */
    private Set<User> loadParticipants(Set<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new BusinessRuleException("Un rendez-vous doit compter au moins un participant");
        }
        List<User> participants = userRepository.findAllById(participantIds);
        if (participants.size() != participantIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs participants sont introuvables");
        }
        participants.stream()
                .filter(user -> !user.isActive())
                .findFirst()
                .ifPresent(user -> {
                    throw new BusinessRuleException(
                            "Le compte de " + user.getFirstName() + " " + user.getLastName() + " est desactive");
                });
        return new HashSet<>(participants);
    }
}
