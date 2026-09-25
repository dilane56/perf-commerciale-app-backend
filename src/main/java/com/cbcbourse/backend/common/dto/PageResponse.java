package com.cbcbourse.backend.common.dto;

import java.util.List;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * Enveloppe de pagination exposee par l'API. On ne serialise pas directement un {@link Page} Spring,
 * dont la representation JSON n'est pas garantie stable d'une version a l'autre : le frontend doit
 * pouvoir compter sur un contrat fixe.
 */
@Schema(description = "Page de resultats")
public record PageResponse<T>(
        @Schema(description = "Elements de la page courante") List<T> contenu,
        @Schema(description = "Numero de page, base zero", example = "0") int page,
        @Schema(description = "Taille de page demandee", example = "20") int taille,
        @Schema(description = "Nombre total d'elements", example = "42") long totalElements,
        @Schema(description = "Nombre total de pages", example = "3") int totalPages
) {
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
