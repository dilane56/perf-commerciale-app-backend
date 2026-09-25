package com.cbcbourse.backend.client;

import java.util.function.Function;

import com.cbcbourse.backend.client.dto.ClientResponse;
import com.cbcbourse.backend.client.dto.CreateClientRequest;
import com.cbcbourse.backend.client.dto.UpdateClientRequest;
import com.cbcbourse.backend.common.dto.PageResponse;
import com.cbcbourse.backend.common.exception.ApiError;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clients", description = "Portefeuille de clients et de prospects de l'equipe commerciale. "
        + "Un commercial ne voit et ne modifie que les clients dont il est le referent.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expire",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Permission manquante ou client hors de son perimetre",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasAnyAuthority('MANAGE_OWN_PORTFOLIO', 'MANAGE_ALL_PORTFOLIOS')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Lister les clients", description = "Liste paginee, restreinte au portefeuille de "
            + "l'utilisateur connecte sauf s'il dispose de MANAGE_ALL_PORTFOLIOS.")
    @GetMapping
    public PageResponse<ClientResponse> listClients(
            @Parameter(description = "Filtrer sur un commercial referent (ignore pour un commercial)", example = "2")
            @RequestParam(required = false) Long referentId,
            @Parameter(description = "Filtrer sur un statut", example = "PROSPECT")
            @RequestParam(required = false) StatutClient statut,
            @Parameter(description = "Filtrer sur une nature juridique", example = "PERSONNE_MORALE")
            @RequestParam(required = false) TypeClient type,
            @Parameter(description = "Recherche sur le nom ou la raison sociale", example = "SODIMA")
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "dateAcquisition") Pageable pageable) {
        return PageResponse.from(clientService.search(referentId, statut, type, recherche, pageable),
                Function.identity());
    }

    @Operation(summary = "Consulter un client")
    @ApiResponse(responseCode = "404", description = "Client introuvable",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public ClientResponse getClient(@PathVariable Long id) {
        return clientService.findById(id);
    }

    @Operation(summary = "Creer un client ou un prospect", description = "Un commercial cree toujours pour "
            + "lui-meme ; seul un detenteur de MANAGE_ALL_PORTFOLIOS peut designer un autre referent.")
    @ApiResponse(responseCode = "400", description = "Donnees invalides",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse created = clientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Modifier un client")
    @PutMapping("/{id}")
    public ClientResponse updateClient(@PathVariable Long id, @Valid @RequestBody UpdateClientRequest request) {
        return clientService.update(id, request);
    }

    @Operation(summary = "Supprimer un client", description = "Refuse si le client porte des transactions : "
            + "celles-ci proviennent d'Atlantis et l'historique ne doit pas disparaitre.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client supprime"),
            @ApiResponse(responseCode = "400", description = "Client porteur de transactions",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PORTFOLIO_DATA')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
