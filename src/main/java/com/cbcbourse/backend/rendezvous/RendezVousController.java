package com.cbcbourse.backend.rendezvous;

import java.time.LocalDate;

import com.cbcbourse.backend.common.dto.PageResponse;
import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.rendezvous.dto.CreateRendezVousRequest;
import com.cbcbourse.backend.rendezvous.dto.RendezVousResponse;
import com.cbcbourse.backend.rendezvous.dto.UpdateRendezVousRequest;

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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rendez-vous", description = "Rendez-vous commerciaux et comptes rendus. Le perimetre suit celui "
        + "du client : un commercial gere les rendez-vous des clients dont il est le referent.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expire",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Permission manquante ou rendez-vous hors de son perimetre",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/rendez-vous")
@PreAuthorize("hasAnyAuthority('MANAGE_OWN_PORTFOLIO', 'MANAGE_ALL_PORTFOLIOS')")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    public RendezVousController(RendezVousService rendezVousService) {
        this.rendezVousService = rendezVousService;
    }

    @Operation(summary = "Lister les rendez-vous", description = "Liste paginee, restreinte aux clients du "
            + "portefeuille de l'utilisateur connecte sauf s'il dispose de MANAGE_ALL_PORTFOLIOS.")
    @GetMapping
    public PageResponse<RendezVousResponse> listRendezVous(
            @Parameter(description = "Filtrer sur un client", example = "1")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "Filtrer sur un participant", example = "2")
            @RequestParam(required = false) Long participantId,
            @Parameter(description = "Debut de periode (inclus)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @Parameter(description = "Fin de periode (incluse)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @PageableDefault(size = 20, sort = "date") Pageable pageable) {
        return PageResponse.from(
                rendezVousService.search(clientId, participantId, debut, fin, pageable),
                RendezVousResponse::from);
    }

    @Operation(summary = "Consulter un rendez-vous")
    @ApiResponse(responseCode = "404", description = "Rendez-vous introuvable",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public RendezVousResponse getRendezVous(@PathVariable Long id) {
        return RendezVousResponse.from(rendezVousService.findById(id));
    }

    @Operation(summary = "Programmer un rendez-vous", description = "La date peut etre dans le futur : "
            + "l'equipe planifie ses visites.")
    @ApiResponse(responseCode = "400", description = "Donnees invalides ou aucun participant",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<RendezVousResponse> createRendezVous(
            @Valid @RequestBody CreateRendezVousRequest request) {
        RendezVous rendezVous = rendezVousService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RendezVousResponse.from(rendezVous));
    }

    @Operation(summary = "Modifier un rendez-vous ou son compte rendu")
    @PutMapping("/{id}")
    public RendezVousResponse updateRendezVous(@PathVariable Long id,
                                               @Valid @RequestBody UpdateRendezVousRequest request) {
        return RendezVousResponse.from(rendezVousService.update(id, request));
    }

    @Operation(summary = "Supprimer un rendez-vous")
    @ApiResponse(responseCode = "204", description = "Rendez-vous supprime")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PORTFOLIO_DATA')")
    public ResponseEntity<Void> deleteRendezVous(@PathVariable Long id) {
        rendezVousService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
