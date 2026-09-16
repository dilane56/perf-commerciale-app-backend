INSERT INTO roles (name, description) VALUES
    (N'ADMIN', N'Administrateur systeme - toutes les permissions'),
    (N'DIRECTION', N'Direction - vue consolidee et pilotage des objectifs'),
    (N'MANAGER_COMMERCIAL', N'Manager commercial - pilotage de son equipe'),
    (N'COMMERCIAL', N'Commercial - suivi de sa propre performance');

-- ADMIN : toutes les permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = N'ADMIN';

-- DIRECTION
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (N'VIEW_ALL_DASHBOARDS', N'MANAGE_OBJECTIVES', N'EXPORT_REPORTS')
WHERE r.name = N'DIRECTION';

-- MANAGER_COMMERCIAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (N'VIEW_TEAM_DASHBOARD', N'MANAGE_OBJECTIVES', N'EXPORT_REPORTS')
WHERE r.name = N'MANAGER_COMMERCIAL';

-- COMMERCIAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (N'VIEW_OWN_DASHBOARD')
WHERE r.name = N'COMMERCIAL';
