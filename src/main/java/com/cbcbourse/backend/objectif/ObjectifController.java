package com.cbcbourse.backend.objectif;

import java.util.List;
import java.util.function.Function;

import com.cbcbourse.backend.auth.dto.AuthenticatedUser;
import com.cbcbourse.backend.common.dto.PageResponse;
import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.objectif.dto.CreateObjectifRequest;
import com.cbcbourse.backend.objectif.dto.ObjectifResponse;
import com.cbcbourse.backend.objectif.dto.ObjectifsEquipeResponse;
import com.cbcbourse.backend.objectif.dto.UpdateObjectifRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Objectifs", description = "Fixation des objectifs commerciaux et suivi du taux d'atteinte, "
        + "calcule a la demande a partir des KPI. Un objectif vise un commercial ou l'equipe d'un "
        + "responsable, jamais les deux.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expire",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Permission manquante ou objectif hors de son perimetre",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/objectifs")
public class ObjectifController {

    private final ObjectifService objectifService;

    public ObjectifController(ObjectifService objectifService) {
        this.objectifService = objectifService;
    }

    @Operation(summary = "Mes objectifs individuels", description = "Objectifs fixes pour l'utilisateur "
            + "connecte, avec le taux d'atteinte calcule sur la periode propre a chacun.")
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VIEW_OWN_DASHBOARD')")
    public List<ObjectifResponse> mesObjectifs(@AuthenticationPrincipal AuthenticatedUser principal) {
        return objectifService.objectifsDuCommercial(principal.id());
    }

    @Operation(summary = "Objectifs de mon equipe", description = "Objectif(s) d'equipe du responsable "
            + "connecte, ainsi que les objectifs individuels de ses commerciaux.")
    @GetMapping("/equipe")
    @PreAuthorize("hasAuthority('VIEW_TEAM_DASHBOARD')")
    public ObjectifsEquipeResponse objectifsDeMonEquipe(@AuthenticationPrincipal AuthenticatedUser principal) {
        return objectifService.objectifsEquipe(principal.id());
    }

    @Operation(summary = "Lister tous les objectifs", description = "Vue consolidee, reservee a la direction.")
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_DASHBOARDS')")
    public PageResponse<ObjectifResponse> listerTout(
            @Parameter(description = "Filtrer sur un commercial", example = "2")
            @RequestParam(required = false) Long commercialId,
            @Parameter(description = "Filtrer sur l'equipe d'un responsable", example = "3")
            @RequestParam(required = false) Long managerId,
            @Parameter(description = "Filtrer sur une metrique", example = "NOUVEAUX_CLIENTS")
            @RequestParam(required = false) TypeObjectif type,
            @PageableDefault(size = 20, sort = "dateDebut") Pageable pageable) {
        return PageResponse.from(objectifService.search(commercialId, managerId, type, pageable),
                Function.identity());
    }

    @Operation(summary = "Fixer un objectif", description = "Reserve a MANAGE_OBJECTIVES (sa propre "
            + "equipe) ou MANAGE_ALL_OBJECTIVES (toute l'entreprise).")
    @ApiResponse(responseCode = "400", description = "Donnees invalides",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_OBJECTIVES', 'MANAGE_ALL_OBJECTIVES')")
    public ResponseEntity<ObjectifResponse> creer(@Valid @RequestBody CreateObjectifRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objectifService.create(request));
    }

    @Operation(summary = "Modifier un objectif")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_OBJECTIVES', 'MANAGE_ALL_OBJECTIVES')")
    public ObjectifResponse modifier(@PathVariable Long id, @Valid @RequestBody UpdateObjectifRequest request) {
        return objectifService.update(id, request);
    }

    @Operation(summary = "Supprimer un objectif")
    @ApiResponse(responseCode = "204", description = "Objectif supprime")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_OBJECTIVES', 'MANAGE_ALL_OBJECTIVES')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        objectifService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
