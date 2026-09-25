-- Objectifs commerciaux : une cible fixee sur une periode, individuelle (commercial_id) ou d'equipe
-- (manager_id). Un objectif d'equipe est une valeur propre fixee par le responsable, pas mecaniquement
-- la somme des objectifs individuels de ses commerciaux. La valeur reelle et le taux d'atteinte ne sont
-- pas stockes : ils sont recalcules a la demande a partir des memes tables que les KPI (clients,
-- transactions, rendez-vous), pour ne jamais avoir deux facons de compter la meme chose.

CREATE TABLE objectifs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    type            NVARCHAR(30) NOT NULL,
    commercial_id   BIGINT NULL,
    manager_id      BIGINT NULL,
    -- DECIMAL et jamais FLOAT : la meme cible sert aussi bien a compter des clients qu'a viser un
    -- montant en XOF.
    valeur_cible    DECIMAL(18,2) NOT NULL,
    date_debut      DATE NOT NULL,
    date_fin        DATE NOT NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_objectifs_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2 NOT NULL CONSTRAINT DF_objectifs_updated_at DEFAULT (SYSUTCDATETIME()),
    created_by      BIGINT NULL,
    updated_by      BIGINT NULL,
    CONSTRAINT FK_objectifs_commercial FOREIGN KEY (commercial_id) REFERENCES users (id),
    CONSTRAINT FK_objectifs_manager FOREIGN KEY (manager_id) REFERENCES users (id),
    CONSTRAINT FK_objectifs_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT FK_objectifs_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT CK_objectifs_type CHECK (type IN (
        N'NOUVEAUX_CLIENTS', N'MONTANT_COLLECTE', N'MANDATS_SIGNES', N'ACTIVITE_TERRAIN')),
    CONSTRAINT CK_objectifs_periode CHECK (date_fin >= date_debut),
    CONSTRAINT CK_objectifs_valeur_cible CHECK (valeur_cible > 0),
    -- Un objectif vise soit un commercial, soit l'equipe d'un responsable, jamais les deux ni aucun.
    CONSTRAINT CK_objectifs_titulaire CHECK (
        (commercial_id IS NOT NULL AND manager_id IS NULL)
        OR (commercial_id IS NULL AND manager_id IS NOT NULL)
    )
);

CREATE INDEX IX_objectifs_commercial ON objectifs (commercial_id);
CREATE INDEX IX_objectifs_manager ON objectifs (manager_id);
