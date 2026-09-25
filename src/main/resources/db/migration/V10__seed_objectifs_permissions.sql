-- MANAGE_OBJECTIVES (deja seedee en V3) autorise a fixer des objectifs, mais seulement pour sa propre
-- equipe : ce perimetre est verifie en couche service par ObjectifAccess, jamais par un nom de role.
-- MANAGE_ALL_OBJECTIVES etend ce perimetre a tous les commerciaux et equipes, sur le meme principe que
-- MANAGE_ALL_PORTFOLIOS pour le portefeuille.

INSERT INTO permissions (code, description) VALUES
    (N'MANAGE_ALL_OBJECTIVES', N'Fixer les objectifs de tous les commerciaux et de toutes les equipes');

-- ADMIN a recu toutes les permissions via un CROSS JOIN execute une seule fois dans V3 : la permission
-- ajoutee ici doit lui etre rattachee explicitement, sinon l'administrateur se retrouverait sans droit
-- au-dela de sa propre equipe.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = N'MANAGE_ALL_OBJECTIVES'
WHERE r.name = N'ADMIN';

-- La direction fixe les objectifs de toute l'entreprise, pas seulement d'une equipe.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = N'MANAGE_ALL_OBJECTIVES'
WHERE r.name = N'DIRECTION';
