-- Lien manager -> commerciaux, necessaire pour cloisonner les objectifs et tableaux de bord d'equipe.
-- Sans ce lien, VIEW_TEAM_DASHBOARD restait inexploitable (limite connue signalee au module KPI).
-- Colonne nullable : un responsable ou un compte administrateur n'a pas de manager.

ALTER TABLE users ADD
    manager_id BIGINT NULL CONSTRAINT FK_users_manager REFERENCES users (id);
