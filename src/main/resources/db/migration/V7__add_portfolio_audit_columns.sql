-- Tracabilite de la saisie : ces donnees sont entrees a la main et servent a evaluer les personnes
-- qui les saisissent, il faut donc pouvoir repondre a "qui a enregistre ce client, et quand".
-- La table transactions n'est pas concernee : l'application ne l'ecrit pas.
-- Colonnes nullables : les lignes creees avant cette migration n'ont pas d'auteur connu.

ALTER TABLE clients ADD
    created_by BIGINT NULL CONSTRAINT FK_clients_created_by REFERENCES users (id),
    updated_by BIGINT NULL CONSTRAINT FK_clients_updated_by REFERENCES users (id);

ALTER TABLE rendez_vous ADD
    created_by BIGINT NULL CONSTRAINT FK_rendez_vous_created_by REFERENCES users (id),
    updated_by BIGINT NULL CONSTRAINT FK_rendez_vous_updated_by REFERENCES users (id);
