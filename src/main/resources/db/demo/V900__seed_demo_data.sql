-- Jeu de donnees de DEMONSTRATION, charge uniquement avec le profil "dev"
-- (voir spring.flyway.locations dans application-dev.yml). Ne jamais activer en production.
--
-- Il reproduit l'equipe decrite dans CLAUDE.md : deux commerciaux qui prospectent parfois en binome,
-- et un responsable qui les accompagne sur le terrain sans etre referent d'aucun client. Les chiffres
-- sont choisis pour rendre visible la distinction entre propriete du client et participation :
-- sur 2026, Fatou Sow affiche 3 rendez-vous mais 0 nouveau client et 0 montant collecte.
--
-- Mot de passe de tous les comptes de demonstration : Admin@123

DECLARE @pwd NVARCHAR(255) = N'$2a$10$zoM7cZrxi3dZRzVmXlkvHuhBSU6txu9BFu0XuRCFmuGBU2XNSZ5WK';

-- ---------------------------------------------------------------------------
-- Equipe commerciale
-- ---------------------------------------------------------------------------
INSERT INTO users (first_name, last_name, email, password_hash, active)
VALUES (N'Awa', N'Diallo', N'awa.diallo@cbcbourse.local', @pwd, 1);
DECLARE @awa BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO users (first_name, last_name, email, password_hash, active)
VALUES (N'Moussa', N'Kone', N'moussa.kone@cbcbourse.local', @pwd, 1);
DECLARE @moussa BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO users (first_name, last_name, email, password_hash, active)
VALUES (N'Fatou', N'Sow', N'fatou.sow@cbcbourse.local', @pwd, 1);
DECLARE @fatou BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO user_roles (user_id, role_id) SELECT @awa, id FROM roles WHERE name = N'COMMERCIAL';
INSERT INTO user_roles (user_id, role_id) SELECT @moussa, id FROM roles WHERE name = N'COMMERCIAL';
INSERT INTO user_roles (user_id, role_id) SELECT @fatou, id FROM roles WHERE name = N'MANAGER_COMMERCIAL';

-- ---------------------------------------------------------------------------
-- Portefeuille de clients (referent unique par client)
-- ---------------------------------------------------------------------------
INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_PHYSIQUE', N'Ibrahim Traore', NULL, @awa, '2026-01-15', N'CLIENT_ACTIF');
DECLARE @c1 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_MORALE', NULL, N'SODIMA SA', @awa, '2026-03-02', N'CLIENT_ACTIF');
DECLARE @c2 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_PHYSIQUE', N'Mariam Cisse', NULL, @awa, '2026-06-10', N'PROSPECT');
DECLARE @c3 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_MORALE', NULL, N'Groupe Kabore SARL', @moussa, '2026-02-20', N'CLIENT_ACTIF');
DECLARE @c4 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_PHYSIQUE', N'Jean-Baptiste Ouedraogo', NULL, @moussa, '2026-05-05', N'CLIENT_ACTIF');
DECLARE @c5 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

-- Client acquis en 2025 : reste hors des KPI 2026, ce qui permet de verifier le filtre de periode.
INSERT INTO clients (type, nom, raison_sociale, commercial_referent_id, date_acquisition, statut)
VALUES (N'PERSONNE_PHYSIQUE', N'Aminata Barry', NULL, @moussa, '2025-11-12', N'CLIENT_INACTIF');
DECLARE @c6 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);

-- ---------------------------------------------------------------------------
-- Rendez-vous et participations
-- ---------------------------------------------------------------------------
INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c1, '2026-01-10', N'PROSPECTION', N'Premier contact, profil de risque equilibre. Responsable present.');
DECLARE @rv1 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv1, @awa), (@rv1, @fatou);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c1, '2026-01-15', N'SIGNATURE', N'Ouverture du compte titres signee.');
DECLARE @rv2 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv2, @awa);

-- Prospection en binome sur une personne morale : les deux commerciaux etaient presents,
-- mais le client reste attribue a son referent (Awa).
INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c2, '2026-02-25', N'PROSPECTION', N'Presentation de l offre de gestion sous mandat au directoire.');
DECLARE @rv3 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv3, @awa), (@rv3, @moussa), (@rv3, @fatou);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c3, '2026-06-05', N'PROSPECTION', N'Interessee, relance prevue apres la recolte.');
DECLARE @rv4 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv4, @awa);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c4, '2026-02-15', N'PROSPECTION', N'Negociation des frais de gestion, responsable present.');
DECLARE @rv5 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv5, @moussa), (@rv5, @fatou);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c4, '2026-03-01', N'SIGNATURE', N'Mandat de gestion signe.');
DECLARE @rv6 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv6, @moussa);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c5, '2026-05-02', N'SUIVI', N'Point trimestriel sur le portefeuille.');
DECLARE @rv7 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv7, @moussa);

INSERT INTO rendez_vous (client_id, date, type, compte_rendu)
VALUES (@c5, '2026-09-10', N'SUIVI', N'Arbitrage discute, accompagnement par une collegue.');
DECLARE @rv8 BIGINT = CAST(SCOPE_IDENTITY() AS BIGINT);
INSERT INTO rendez_vous_participants (rendez_vous_id, user_id) VALUES (@rv8, @moussa), (@rv8, @awa);

-- ---------------------------------------------------------------------------
-- Transactions (montants en XOF)
-- ---------------------------------------------------------------------------
INSERT INTO transactions (client_id, type, montant, date_transaction, description, source_systeme) VALUES
    (@c1, N'INTERMEDIATION',      4500000.00, '2026-02-01', N'Achat d actions cotees BRVM',        N'MANUEL'),
    (@c1, N'INTERMEDIATION',      1250000.00, '2026-07-15', N'Souscription obligataire',           N'MANUEL'),
    (@c2, N'GESTION_SOUS_MANDAT',15000000.00, '2026-03-10', N'Dotation initiale du mandat',        N'MANUEL'),
    (@c4, N'GESTION_SOUS_MANDAT',22000000.00, '2026-03-01', N'Dotation initiale du mandat',        N'MANUEL'),
    (@c4, N'INTERMEDIATION',      3000000.00, '2026-08-20', N'Ordre d achat sur titres regionaux', N'MANUEL'),
    (@c5, N'INTERMEDIATION',       750000.00, '2026-05-20', N'Premier ordre de bourse',            N'MANUEL'),
    -- Hors periode 2026 : sert a verifier que le filtre de date exclut bien l historique.
    (@c6, N'INTERMEDIATION',       500000.00, '2025-12-01', N'Ordre passe avant mise en sommeil',  N'MANUEL');
