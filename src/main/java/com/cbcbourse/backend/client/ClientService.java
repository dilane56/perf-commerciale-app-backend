package com.cbcbourse.backend.client;

import java.time.LocalDate;

import com.cbcbourse.backend.client.dto.CreateClientRequest;
import com.cbcbourse.backend.client.dto.UpdateClientRequest;
import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.common.security.PortfolioAccess;
import com.cbcbourse.backend.transaction.TransactionRepository;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion du portefeuille de clients et de prospects.
 *
 * <p>Chaque operation applique le cloisonnement decrit dans CLAUDE.md : un commercial ne voit et ne
 * modifie que les clients dont il est le referent, le responsable et la direction agissent sur tout
 * le portefeuille. Ce controle est fait ici, en plus du controle de permission assure par Spring
 * Security sur le controleur : la permission dit ce qu'on peut faire, ce service dit sur quelles lignes.
 */
@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioAccess portfolioAccess;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository,
                         TransactionRepository transactionRepository, PortfolioAccess portfolioAccess) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.portfolioAccess = portfolioAccess;
    }

    @Transactional(readOnly = true)
    public Page<Client> search(Long referentId, StatutClient statut, TypeClient type, String recherche,
                               Pageable pageable) {
        Specification<Client> spec = Specification.unrestricted();

        if (referentId != null) {
            spec = spec.and(ClientSpecifications.hasReferent(referentId));
        }
        if (!portfolioAccess.canManageAllPortfolios()) {
            // Le perimetre s'ajoute au filtre demande au lieu de le remplacer : un commercial qui
            // reclame le portefeuille d'un collegue obtient une liste vide, et non le sien par
            // substitution silencieuse.
            spec = spec.and(ClientSpecifications.hasReferent(portfolioAccess.currentUserId()));
        }

        if (statut != null) {
            spec = spec.and(ClientSpecifications.hasStatut(statut));
        }
        if (type != null) {
            spec = spec.and(ClientSpecifications.hasType(type));
        }
        if (recherche != null && !recherche.isBlank()) {
            spec = spec.and(ClientSpecifications.designationContient(recherche.trim()));
        }

        return clientRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Client findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable: " + id));
        portfolioAccess.checkCanActOnPortfolioOf(client.getCommercialReferent().getId());
        return client;
    }

    public Client create(CreateClientRequest request) {
        Long referentId = portfolioAccess.resolveReferentId(request.commercialReferentId());

        Client client = new Client();
        client.setType(request.type());
        client.setCommercialReferent(loadReferent(referentId));
        client.setDateAcquisition(request.dateAcquisition());
        client.setStatut(request.statut());
        appliquerDesignation(client, request.type(), request.nom(), request.raisonSociale());
        verifierDateAcquisition(request.dateAcquisition());

        client.setCreatedBy(portfolioAccess.currentUserId());
        client.setUpdatedBy(portfolioAccess.currentUserId());
        return clientRepository.save(client);
    }

    public Client update(Long id, UpdateClientRequest request) {
        Client client = findById(id);

        if (request.commercialReferentId() != null
                && !request.commercialReferentId().equals(client.getCommercialReferent().getId())) {
            // Reaffecter un client deplace ses clients et ses montants d'un commercial a l'autre
            // dans les KPI : ce n'est pas une correction anodine.
            Long nouveauReferent = portfolioAccess.resolveReferentId(request.commercialReferentId());
            client.setCommercialReferent(loadReferent(nouveauReferent));
        }

        client.setType(request.type());
        client.setDateAcquisition(request.dateAcquisition());
        client.setStatut(request.statut());
        appliquerDesignation(client, request.type(), request.nom(), request.raisonSociale());
        verifierDateAcquisition(request.dateAcquisition());

        client.setUpdatedBy(portfolioAccess.currentUserId());
        return clientRepository.save(client);
    }

    public void delete(Long id) {
        Client client = findById(id);
        if (transactionRepository.existsByClientId(id)) {
            // Les transactions viennent d'Atlantis : cette application n'a pas a faire disparaitre
            // un client effectif. Le passer en CLIENT_INACTIF est la bonne reponse.
            throw new BusinessRuleException("Impossible de supprimer ce client : il porte des transactions. "
                    + "Passez son statut a CLIENT_INACTIF.");
        }
        clientRepository.delete(client);
    }

    private User loadReferent(Long referentId) {
        User referent = userRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial referent introuvable: " + referentId));
        if (!referent.isActive()) {
            throw new BusinessRuleException("Le commercial referent designe est desactive");
        }
        return referent;
    }

    /**
     * Une personne physique porte un nom et pas de raison sociale, une personne morale l'inverse. La
     * base l'impose deja via CK_clients_designation, mais l'API doit rendre une erreur lisible plutot
     * qu'une violation SQL.
     */
    private void appliquerDesignation(Client client, TypeClient type, String nom, String raisonSociale) {
        if (type == TypeClient.PERSONNE_PHYSIQUE) {
            if (nom == null || nom.isBlank()) {
                throw new BusinessRuleException("Le nom est obligatoire pour une personne physique");
            }
            client.setNom(nom.trim());
            client.setRaisonSociale(null);
        } else {
            if (raisonSociale == null || raisonSociale.isBlank()) {
                throw new BusinessRuleException("La raison sociale est obligatoire pour une personne morale");
            }
            client.setRaisonSociale(raisonSociale.trim());
            client.setNom(null);
        }
    }

    private void verifierDateAcquisition(LocalDate dateAcquisition) {
        if (dateAcquisition.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La date d'acquisition ne peut pas etre dans le futur");
        }
    }
}
