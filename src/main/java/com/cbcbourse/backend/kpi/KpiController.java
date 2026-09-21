package com.cbcbourse.backend.kpi;

import java.time.LocalDate;

import com.cbcbourse.backend.auth.dto.AuthenticatedUser;
import com.cbcbourse.backend.common.exception.ApiError;
import com.cbcbourse.backend.kpi.dto.KpiCommercialResponse;
import com.cbcbourse.backend.kpi.dto.KpiPeriodeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "KPI", description = "Indicateurs de performance commerciale, calcules a la demande a partir des clients, "
        + "rendez-vous et transactions. Lecture seule : la saisie des donnees fait l'objet d'un module distinct.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/kpi")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @Operation(summary = "Mes indicateurs", description = "Indicateurs de l'utilisateur connecte sur la periode demandee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicateurs calcules",
                    content = @Content(schema = @Schema(implementation = KpiCommercialResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expire",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permission VIEW_OWN_DASHBOARD requise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VIEW_OWN_DASHBOARD')")
    public KpiCommercialResponse mesKpis(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = "Debut de periode (inclus)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @Parameter(description = "Fin de periode (incluse)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return kpiService.forUser(principal.id(), debut, fin);
    }

    @Operation(summary = "Vue consolidee", description = "Indicateurs de tous les commerciaux suivis, avec les totaux de l'equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicateurs calcules",
                    content = @Content(schema = @Schema(implementation = KpiPeriodeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permission VIEW_ALL_DASHBOARDS requise",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/commerciaux")
    @PreAuthorize("hasAuthority('VIEW_ALL_DASHBOARDS')")
    public KpiPeriodeResponse kpisConsolides(
            @Parameter(description = "Debut de periode (inclus)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @Parameter(description = "Fin de periode (incluse)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return kpiService.forAllCommerciaux(debut, fin);
    }

    @Operation(summary = "Indicateurs d'un commercial", description = "Indicateurs d'un commercial designe par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicateurs calcules",
                    content = @Content(schema = @Schema(implementation = KpiCommercialResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permission VIEW_ALL_DASHBOARDS requise",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/commerciaux/{userId}")
    @PreAuthorize("hasAuthority('VIEW_ALL_DASHBOARDS')")
    public KpiCommercialResponse kpisCommercial(
            @Parameter(description = "Identifiant du commercial", example = "2") @PathVariable Long userId,
            @Parameter(description = "Debut de periode (inclus)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @Parameter(description = "Fin de periode (incluse)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return kpiService.forUser(userId, debut, fin);
    }
}
