package com.cbcbourse.backend.common.exception;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structure d'erreur standard renvoyee par l'API")
public record ApiError(
        @Schema(description = "Horodatage de l'erreur (UTC)") Instant timestamp,
        @Schema(description = "Code HTTP", example = "400") int status,
        @Schema(description = "Libelle du statut HTTP", example = "Bad Request") String error,
        @Schema(description = "Message d'erreur", example = "Erreur de validation") String message,
        @Schema(description = "Details complementaires (ex: erreurs de validation par champ)") List<String> details
) {
}
