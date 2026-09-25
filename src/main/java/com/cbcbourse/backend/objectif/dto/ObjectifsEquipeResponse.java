package com.cbcbourse.backend.objectif.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objectifs d'une equipe : l'objectif propre a l'equipe et les objectifs "
        + "individuels de ses membres, chacun avec son propre taux d'atteinte")
public record ObjectifsEquipeResponse(
        List<ObjectifResponse> objectifsEquipe,
        List<ObjectifResponse> objectifsIndividuels
) {
}
