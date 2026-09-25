-- Permissions du module de gestion du portefeuille commercial (clients/prospects et rendez-vous).
-- Aucune permission d'ecriture sur les transactions n'est creee : elles relevent d'Atlantis SGI.

INSERT INTO permissions (code, description) VALUES
    (N'MANAGE_OWN_PORTFOLIO',  N'Gerer ses propres clients/prospects et leurs rendez-vous'),
    (N'MANAGE_ALL_PORTFOLIOS', N'Gerer les clients et rendez-vous de toute l equipe commerciale'),
    (N'DELETE_PORTFOLIO_DATA', N'Supprimer un client sans historique ou un rendez-vous');

-- ADMIN a recu toutes les permissions via un CROSS JOIN execute une seule fois dans V3 : les
-- permissions ajoutees ici doivent lui etre rattachees explicitement, sinon l'administrateur se
-- retrouverait sans droit sur le portefeuille.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (N'MANAGE_OWN_PORTFOLIO', N'MANAGE_ALL_PORTFOLIOS', N'DELETE_PORTFOLIO_DATA')
WHERE r.name = N'ADMIN';

-- Un commercial gere son propre portefeuille.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = N'MANAGE_OWN_PORTFOLIO'
WHERE r.name = N'COMMERCIAL';

-- Le responsable et la direction gerent celui de toute l equipe.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (N'MANAGE_ALL_PORTFOLIOS', N'DELETE_PORTFOLIO_DATA')
WHERE r.name IN (N'MANAGER_COMMERCIAL', N'DIRECTION');
