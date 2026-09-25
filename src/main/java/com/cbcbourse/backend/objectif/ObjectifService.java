package com.cbcbourse.backend.objectif;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cbcbourse.backend.common.exception.BusinessRuleException;
import com.cbcbourse.backend.common.exception.ResourceNotFoundException;
import com.cbcbourse.backend.common.security.ObjectifAccess;
import com.cbcbourse.backend.kpi.KpiService;
import com.cbcbourse.backend.kpi.dto.KpiCommercialResponse;
import com.cbcbourse.backend.objectif.dto.CreateObjectifRequest;
import com.cbcbourse.backend.objectif.dto.ObjectifResponse;
import com.cbcbourse.backend.objectif.dto.ObjectifsEquipeResponse;
import com.cbcbourse.backend.objectif.dto.UpdateObjectifRequest;
import com.cbcbourse.backend.user.User;
import com.cbcbourse.backend.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixation des objectifs commerciaux et suivi du taux d'atteinte.
 *
 * <p>La valeur reelle n'est jamais stockee : elle est recalculee a la demande via {@link KpiService},
 * exactement comme les tableaux de bord, pour ne jamais avoir deux facons de compter la meme chose. Les
 * reponses sont assemblees ici, a l'interieur de la transaction, pour pouvoir lire les noms du
 * commercial et du responsable sans depasser la duree de vie de la session JPA.
 */
@Service
@Transactional
public class ObjectifService {

    private final ObjectifRepository objectifRepository;
    private final UserRepository userRepository;
    private final KpiService kpiService;
    private final ObjectifAccess objectifAccess;

    public ObjectifService(ObjectifRepository objectifRepository, UserRepository userRepository,
                           KpiService kpiService, ObjectifAccess objectifAccess) {
        this.objectifRepository = objectifRepository;
        this.userRepository = userRepository;
        this.kpiService = kpiService;
        this.objectifAccess = objectifAccess;
    }

    @Transactional(readOnly = true)
    public Page<ObjectifResponse> search(Long commercialId, Long managerId, TypeObjectif type, Pageable pageable) {
        Specification<Objectif> spec = Specification.unrestricted();
        if (commercialId != null) {
            spec = spec.and(ObjectifSpecifications.pourCommercial(commercialId));
        }
        if (managerId != null) {
            spec = spec.and(ObjectifSpecifications.pourManager(managerId));
        }
        if (type != null) {
            spec = spec.and(ObjectifSpecifications.deType(type));
        }
        return objectifRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ObjectifResponse> objectifsDuCommercial(Long commercialId) {
        return objectifRepository.findAll(ObjectifSpecifications.pourCommercial(commercialId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ObjectifsEquipeResponse objectifsEquipe(Long managerId) {
        List<ObjectifResponse> objectifsEquipe = objectifRepository
                .findAll(ObjectifSpecifications.pourManager(managerId)).stream()
                .map(this::toResponse)
                .toList();

        List<Long> membreIds = userRepository.findByManagerId(managerId).stream().map(User::getId).toList();
        List<ObjectifResponse> objectifsIndividuels = membreIds.isEmpty()
                ? List.of()
                : objectifRepository.findAll(ObjectifSpecifications.pourCommerciaux(membreIds)).stream()
                        .map(this::toResponse)
                        .toList();

        return new ObjectifsEquipeResponse(objectifsEquipe, objectifsIndividuels);
    }

    public ObjectifResponse create(CreateObjectifRequest request) {
        verifierPeriode(request.dateDebut(), request.dateFin());
        Objectif objectif = new Objectif();
        objectif.setType(request.type());
        assignerTitulaire(objectif, request.commercialId(), request.managerId());
        objectif.setValeurCible(request.valeurCible());
        objectif.setDateDebut(request.dateDebut());
        objectif.setDateFin(request.dateFin());
        objectif.setCreatedBy(objectifAccess.currentUserId());
        objectif.setUpdatedBy(objectifAccess.currentUserId());
        return toResponse(objectifRepository.save(objectif));
    }

    public ObjectifResponse update(Long id, UpdateObjectifRequest request) {
        verifierPeriode(request.dateDebut(), request.dateFin());
        Objectif objectif = getOrThrow(id);
        // Verifie l'acces sur l'affectation actuelle avant de la changer : sans ce controle, un
        // responsable pourrait s'approprier l'objectif d'une autre equipe en le reaffectant a la sienne.
        verifierPerimetreActuel(objectif);
        objectif.setType(request.type());
        assignerTitulaire(objectif, request.commercialId(), request.managerId());
        objectif.setValeurCible(request.valeurCible());
        objectif.setDateDebut(request.dateDebut());
        objectif.setDateFin(request.dateFin());
        objectif.setUpdatedBy(objectifAccess.currentUserId());
        return toResponse(objectifRepository.save(objectif));
    }

    public void delete(Long id) {
        Objectif objectif = getOrThrow(id);
        verifierPerimetreActuel(objectif);
        objectifRepository.delete(objectif);
    }

    private Objectif getOrThrow(Long id) {
        return objectifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objectif introuvable: " + id));
    }

    private void assignerTitulaire(Objectif objectif, Long commercialId, Long managerId) {
        if ((commercialId == null) == (managerId == null)) {
            throw new BusinessRuleException(
                    "Un objectif vise soit un commercial, soit l'equipe d'un responsable, jamais les deux");
        }
        if (commercialId != null) {
            User commercial = loadActiveUser(commercialId, "Commercial introuvable: " + commercialId);
            Long managerDuCommercial = commercial.getManager() != null ? commercial.getManager().getId() : null;
            objectifAccess.checkCanManageObjectiveOfCommercial(managerDuCommercial);
            objectif.setCommercial(commercial);
            objectif.setManager(null);
        } else {
            User manager = loadActiveUser(managerId, "Responsable introuvable: " + managerId);
            objectifAccess.checkCanManageObjectiveOfTeam(manager.getId());
            objectif.setManager(manager);
            objectif.setCommercial(null);
        }
    }

    private void verifierPerimetreActuel(Objectif objectif) {
        if (objectif.getCommercial() != null) {
            User commercial = objectif.getCommercial();
            Long managerDuCommercial = commercial.getManager() != null ? commercial.getManager().getId() : null;
            objectifAccess.checkCanManageObjectiveOfCommercial(managerDuCommercial);
        } else {
            objectifAccess.checkCanManageObjectiveOfTeam(objectif.getManager().getId());
        }
    }

    private User loadActiveUser(Long id, String messageSiAbsent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageSiAbsent));
        if (!user.isActive()) {
            throw new BusinessRuleException("Impossible de fixer un objectif pour un compte desactive");
        }
        return user;
    }

    private void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new BusinessRuleException("La date de fin ne peut pas preceder la date de debut");
        }
    }

    private ObjectifResponse toResponse(Objectif objectif) {
        BigDecimal valeurReelle = objectif.getCommercial() != null
                ? valeurReelleCommercial(objectif)
                : valeurReelleEquipe(objectif);
        return ObjectifResponse.of(objectif, valeurReelle);
    }

    private BigDecimal valeurReelleCommercial(Objectif objectif) {
        KpiCommercialResponse kpi = kpiService.forUser(
                objectif.getCommercial().getId(), objectif.getDateDebut(), objectif.getDateFin());
        return extraire(objectif.getType(), kpi);
    }

    /**
     * Somme des valeurs individuelles des membres de l'equipe : l'objectif d'equipe reste une cible
     * propre, mais sa valeur reelle agrege l'activite de ceux qui la composent.
     */
    private BigDecimal valeurReelleEquipe(Objectif objectif) {
        return userRepository.findByManagerId(objectif.getManager().getId()).stream()
                .map(membre -> kpiService.forUser(membre.getId(), objectif.getDateDebut(), objectif.getDateFin()))
                .map(kpi -> extraire(objectif.getType(), kpi))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal extraire(TypeObjectif type, KpiCommercialResponse kpi) {
        return switch (type) {
            case NOUVEAUX_CLIENTS -> BigDecimal.valueOf(kpi.nouveauxClients());
            case MONTANT_COLLECTE -> kpi.montantCollecte();
            case MANDATS_SIGNES -> BigDecimal.valueOf(kpi.mandatsSignes());
            case ACTIVITE_TERRAIN -> BigDecimal.valueOf(kpi.rendezVous());
        };
    }
}
