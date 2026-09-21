-- Modele d'evenements bruts servant de base aux KPI commerciaux.
-- Les indicateurs ne sont pas stockes : ils sont recalcules a la demande a partir de ces tables.

CREATE TABLE clients (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    type                    NVARCHAR(30) NOT NULL,
    nom                     NVARCHAR(255) NULL,
    raison_sociale          NVARCHAR(255) NULL,
    -- Referent unique : c'est lui qui recoit le credit du client dans les KPI individuels.
    commercial_referent_id  BIGINT NOT NULL,
    date_acquisition        DATE NOT NULL,
    statut                  NVARCHAR(30) NOT NULL CONSTRAINT DF_clients_statut DEFAULT (N'PROSPECT'),
    created_at              DATETIME2 NOT NULL CONSTRAINT DF_clients_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at              DATETIME2 NOT NULL CONSTRAINT DF_clients_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_clients_referent FOREIGN KEY (commercial_referent_id) REFERENCES users (id),
    CONSTRAINT CK_clients_type CHECK (type IN (N'PERSONNE_PHYSIQUE', N'PERSONNE_MORALE')),
    CONSTRAINT CK_clients_statut CHECK (statut IN (N'PROSPECT', N'CLIENT_ACTIF', N'CLIENT_INACTIF')),
    -- Une personne physique porte un nom, une personne morale une raison sociale.
    CONSTRAINT CK_clients_designation CHECK (
        (type = N'PERSONNE_PHYSIQUE' AND nom IS NOT NULL)
        OR (type = N'PERSONNE_MORALE' AND raison_sociale IS NOT NULL)
    )
);

-- Couvre le KPI "nouveaux clients par commercial sur une periode".
CREATE INDEX IX_clients_referent_acquisition ON clients (commercial_referent_id, date_acquisition);

CREATE TABLE rendez_vous (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    client_id       BIGINT NOT NULL,
    date            DATE NOT NULL,
    type            NVARCHAR(30) NOT NULL,
    compte_rendu    NVARCHAR(MAX) NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_rendez_vous_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_rendez_vous_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT CK_rendez_vous_type CHECK (type IN (N'PROSPECTION', N'SUIVI', N'SIGNATURE', N'AUTRE'))
);

CREATE INDEX IX_rendez_vous_client_date ON rendez_vous (client_id, date);

-- Plusieurs personnes assistent souvent au meme rendez-vous (binome, responsable accompagnant) :
-- la participation est donc une table de liaison, jamais une cle etrangere unique sur rendez_vous.
CREATE TABLE rendez_vous_participants (
    rendez_vous_id  BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    CONSTRAINT PK_rendez_vous_participants PRIMARY KEY (rendez_vous_id, user_id),
    CONSTRAINT FK_rvp_rendez_vous FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous (id) ON DELETE CASCADE,
    CONSTRAINT FK_rvp_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Couvre le KPI "activite terrain" interroge par utilisateur.
CREATE INDEX IX_rvp_user ON rendez_vous_participants (user_id);

CREATE TABLE transactions (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    client_id           BIGINT NOT NULL,
    type                NVARCHAR(30) NOT NULL,
    -- DECIMAL et jamais FLOAT : montants financiers, en XOF/FCFA (devise unique pour l'instant).
    montant             DECIMAL(18,2) NOT NULL,
    date_transaction    DATE NOT NULL,
    description         NVARCHAR(500) NULL,
    -- Prepare l'integration Atlantis ; un source_reference_id pourra s'ajouter plus tard.
    source_systeme      NVARCHAR(30) NOT NULL CONSTRAINT DF_transactions_source DEFAULT (N'MANUEL'),
    created_at          DATETIME2 NOT NULL CONSTRAINT DF_transactions_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_transactions_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT CK_transactions_type CHECK (type IN (N'INTERMEDIATION', N'GESTION_SOUS_MANDAT')),
    CONSTRAINT CK_transactions_source CHECK (source_systeme IN (N'MANUEL', N'ATLANTIS'))
);

CREATE INDEX IX_transactions_client_date ON transactions (client_id, date_transaction);
