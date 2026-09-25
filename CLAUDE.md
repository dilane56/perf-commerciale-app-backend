# Contexte du projet — Application de Performance Commerciale

## 1. Contexte métier

L'entreprise est une **Société de Gestion et d'Intermédiation (SGI)**, active sur le marché financier régional (zone UEMOA), avec deux activités principales :

- **Intermédiation sur le marché financier** (achat/vente de valeurs mobilières pour le compte de clients)
- **Gestion sous mandat** (gestion déléguée de portefeuille selon le profil de risque du client)

L'entreprise utilise déjà un logiciel métier existant, **Atlantis SGI** (édité par KCS SARL), qui gère : comptes clients (personnes physiques et morales), portefeuilles, ordres de bourse, transactions, souscriptions, mandats de gestion, facturation (droits de garde, frais de gestion, commissions), et dispose d'un module de reporting interne.

**Important** : l'intégration technique avec Atlantis SGI (accès base de données, formats d'export du module Reporting, structure exacte des données) n'est **pas encore confirmée**. Ces informations seront obtenues lors d'un séminaire de formation/briefing à venir. En attendant, le projet doit être conçu de façon **découplée** de toute source de données spécifique, pour pouvoir brancher Atlantis (ou une saisie manuelle en attendant) sans réécrire l'architecture.

## 2. Objectif de l'application

Construire une application permettant au **service commercial** de l'entreprise d'évaluer sa performance :

- Nombre de nouveaux clients (personnes physiques/morales) par commercial
- Volume d'actifs collectés (AUM), mandats de gestion signés
- Chiffre d'affaires généré (commissions, frais de gestion)
- Taux d'atteinte des objectifs individuels et d'équipe
- Tableaux de bord individuels (par commercial) et managériaux (vue consolidée)

## 3. Roadmap générale (pour contexte — ne pas développer tout de suite)

1. ✅ **Terminé : Utilisateurs, Rôles & Permissions**
2. ✅ **Terminé : Modèle de données KPI générique** (entités d'événements bruts + endpoints de lecture des KPI)
3. ✅ **Terminé : Gestion du portefeuille commercial** (clients/prospects et rendez-vous)
4. ✅ **Terminé : Objectifs commerciaux** (fixation + suivi du taux d'atteinte)
5. ✅ **Terminé côté backend : Tableaux de bord** (vue individuelle / vue managériale) — objet de ce document
6. Intégration Atlantis SGI (une fois les modalités d'accès connues)

Ce fichier se concentre sur le **point 5**. Les points 1 à 4 sont déjà implémentés — ne pas les reconstruire.
Les sections 5, 6, 7 et 8 décrivent ce qui existe déjà et restent dans ce fichier à titre de référence : le
module Tableaux de bord (section 9) ne fait qu'ajouter le point d'entrée KPI d'équipe manquant, en
s'appuyant sur le lien manager → commerciaux introduit par le module Objectifs (section 8.2) et sur les
permissions du RBAC (section 5). L'entité `Transaction` relève d'Atlantis SGI et n'est écrite par aucun
module de cette application (voir section 7.2).

---

## 4. Stack technique

| Composant | Choix |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| Sécurité | Spring Security (JWT pour l'authentification stateless) |
| Persistance | Spring Data JPA / Hibernate |
| Base de données | SQL Server (SGBD déjà utilisé par l'entreprise) |
| Migrations DB | Flyway (support SQL Server via le driver `flyway-sqlserver`) |
| Frontend | Next.js (React), TypeScript |
| Appels API frontend → backend | REST (JSON), fetch/axios |
| Style | Tailwind CSS (à confirmer si tu as une autre préférence) |

---

## 5. Module livré (référence) : Gestion des Utilisateurs, Rôles & Permissions (RBAC)

> **État** : livré. Authentification JWT (`/api/auth/login`, `/refresh`, `/logout`, `/me`), CRUD utilisateurs
> et rôles, consultation des permissions, enforcement par `@PreAuthorize("hasAuthority(...)")`. Une requête
> sans token valide reçoit un **401**, une requête authentifiée sans la permission requise un **403**, toutes
> deux avec un corps `ApiError`. Cette section reste ici comme référence du modèle de permissions.

### 5.1. Objectif du module

Mettre en place un système d'authentification et de contrôle d'accès **configurable depuis l'interface** (pas codé en dur), sur le modèle RBAC (Role-Based Access Control). Un administrateur doit pouvoir créer des rôles, leur assigner des permissions, et assigner des rôles à des utilisateurs, sans toucher au code ni redéployer l'application.

### 5.2. Modèle de données

**Entité `User`**
- `id` (UUID ou Long, clé primaire)
- `firstName`, `lastName`
- `email` (unique, sert d'identifiant de connexion)
- `passwordHash` (jamais stocker en clair — BCrypt)
- `active` (boolean — permet de désactiver un compte sans le supprimer)
- `createdAt`, `updatedAt`

**Entité `Role`**
- `id`
- `name` (ex : `COMMERCIAL`, `MANAGER_COMMERCIAL`, `DIRECTION`, `ADMIN`)
- `description`
- `createdAt`, `updatedAt`

**Entité `Permission`**
- `id`
- `code` (ex : `VIEW_OWN_DASHBOARD`, `VIEW_ALL_DASHBOARDS`, `MANAGE_OBJECTIVES`, `EXPORT_REPORTS`, `MANAGE_USERS`, `MANAGE_ROLES`)
- `description`

**Table de liaison `role_permissions`** (many-to-many entre `Role` et `Permission`)

**Table de liaison `user_roles`** (many-to-many entre `User` et `Role` — un utilisateur peut avoir plusieurs rôles)

### 5.3. Fonctionnalités attendues

**Authentification**
- Connexion par email + mot de passe → retourne un JWT
- Endpoint pour rafraîchir le token (refresh token)
- Endpoint de déconnexion (invalidation côté client suffisante pour une v1 stateless ; blacklist de token en option plus tard)

**Gestion des utilisateurs (CRUD)** — réservé aux rôles disposant de la permission `MANAGE_USERS`
- Créer un utilisateur (avec un ou plusieurs rôles assignés)
- Lister les utilisateurs (avec filtre par rôle, statut actif/inactif)
- Modifier un utilisateur (infos, rôles assignés)
- Désactiver/réactiver un utilisateur (pas de suppression physique, pour garder l'historique)
- Réinitialiser le mot de passe d'un utilisateur

**Gestion des rôles (CRUD)** — réservé aux rôles disposant de la permission `MANAGE_ROLES`
- Créer un rôle
- Modifier un rôle (nom, description)
- Assigner/retirer des permissions à un rôle (interface à cocher les permissions)
- Lister les rôles avec leurs permissions
- Empêcher la suppression d'un rôle encore assigné à des utilisateurs (ou demander confirmation explicite)

**Gestion des permissions**
- Les permissions sont définies par le système (liste fixe en base, alimentée par migration Flyway), pas créées librement par l'admin dans une v1 — pour éviter des permissions orphelines non vérifiées dans le code
- Écran de consultation de la liste des permissions disponibles

**Contrôle d'accès (enforcement)**
- Middleware/filtre Spring Security qui vérifie, à chaque requête, que l'utilisateur authentifié possède la permission requise pour l'endpoint appelé
- Les permissions doivent être chargées dans le token JWT (ou récupérées via le contexte de sécurité) pour éviter une requête DB à chaque vérification
- Annotations Spring Security du type `@PreAuthorize("hasAuthority('MANAGE_USERS')")` sur les contrôleurs/méthodes sensibles

### 5.4. Permissions initiales à créer (liste de départ, extensible)

```
MANAGE_USERS          — créer/modifier/désactiver des utilisateurs
MANAGE_ROLES          — créer/modifier des rôles et leurs permissions
VIEW_OWN_DASHBOARD    — voir son propre tableau de bord de performance
VIEW_TEAM_DASHBOARD   — voir le tableau de bord de son équipe
VIEW_ALL_DASHBOARDS   — voir tous les tableaux de bord (vue direction)
MANAGE_OBJECTIVES     — fixer/modifier les objectifs commerciaux
EXPORT_REPORTS        — exporter des rapports
```

### 5.5. Rôles initiaux suggérés (à créer via données de départ / seed)

| Rôle | Permissions |
|---|---|
| `ADMIN` | Toutes les permissions |
| `DIRECTION` | `VIEW_ALL_DASHBOARDS`, `MANAGE_OBJECTIVES`, `EXPORT_REPORTS` |
| `MANAGER_COMMERCIAL` | `VIEW_TEAM_DASHBOARD`, `MANAGE_OBJECTIVES`, `EXPORT_REPORTS` |
| `COMMERCIAL` | `VIEW_OWN_DASHBOARD` |

### 5.6. Structure de projet suggérée

**Backend (Spring Boot)**
```
src/main/java/com/entreprise/perfcommerciale/
├── config/          # SecurityConfig, JwtConfig, CorsConfig
├── auth/            # AuthController, AuthService, JwtUtils
├── user/            # UserController, UserService, UserRepository, User entity
├── role/            # RoleController, RoleService, RoleRepository, Role entity
├── permission/      # PermissionController, PermissionService, Permission entity
└── common/          # exceptions, DTOs communs, mapper utilitaires
src/main/resources/
├── db/migration/    # scripts Flyway (V1__init_schema.sql, V2__seed_roles_permissions.sql...)
└── application.yml
```

**Frontend (Next.js)**
```
app/
├── (auth)/login/           # page de connexion
├── (admin)/users/          # liste + gestion des utilisateurs
├── (admin)/roles/          # liste + gestion des rôles et permissions
├── lib/api.ts              # client HTTP centralisé (attache le JWT)
├── lib/auth-context.tsx    # contexte React pour l'utilisateur connecté + ses permissions
└── components/
```

### 5.7. Points d'attention pour Claude Code

- Ne pas coder les vérifications de permissions "en dur" par rôle (`if role == ADMIN`) — toujours vérifier une **permission**, jamais un rôle directement, pour rester cohérent avec le principe RBAC.
- Le frontend doit aussi masquer/afficher les éléments d'interface selon les permissions de l'utilisateur connecté (récupérées à la connexion), en plus de la vérification côté backend qui reste la seule source de vérité pour la sécurité.
- Prévoir dès le départ une table de migration Flyway qui **seed** les permissions et rôles de base (section 5.4 et 5.5), pour ne pas repartir d'une base vide à chaque environnement.
- Les mots de passe ne doivent jamais transiter ni être stockés en clair (BCrypt côté backend).
- Ce module doit rester indépendant du reste de l'application (KPI, dashboards) — il n'a aucune dépendance vers Atlantis SGI ou les futurs modules métier.
- **Spécificités SQL Server à respecter** :
  - Driver JDBC : `com.microsoft.sqlserver:mssql-jdbc` dans les dépendances Maven/Gradle
  - Dialecte Hibernate : `org.hibernate.dialect.SQLServerDialect`
  - Pour les clés primaires auto-générées, utiliser `IDENTITY` (colonnes `INT`/`BIGINT` avec `@GeneratedValue(strategy = GenerationType.IDENTITY)`) plutôt que des séquences, ou opter pour `UNIQUEIDENTIFIER` (UUID) si l'entreprise a une préférence pour les clés non séquentielles
  - Adapter les scripts Flyway en syntaxe T-SQL (types `NVARCHAR`, `BIT` pour les booléens, `DATETIME2` pour les dates, etc.)

---

## 6. Module livré (référence) : Modèle de données KPI générique

> **État** : livré. Les entités `Client`, `RendezVous` et `Transaction` existent (migration `V5`), avec la
> table de liaison `rendez_vous_participants`. Les KPI sont exposés en lecture seule sur `/api/kpi/me`,
> `/api/kpi/commerciaux` et `/api/kpi/commerciaux/{userId}`, protégés respectivement par `VIEW_OWN_DASHBOARD`
> et `VIEW_ALL_DASHBOARDS`. Un jeu de démonstration est chargé par le seul profil `dev` (`db/demo`).
>
> **Limite levée** : `VIEW_TEAM_DASHBOARD` restait inexploitable faute de notion d'équipe. Le module
> Objectifs (section 8) a ajouté `manager_id` sur `User`, et le module Tableaux de bord (section 9) a
> ajouté `GET /api/kpi/equipe` dessus : l'équipe d'un responsable est l'ensemble des commerciaux actifs
> dont `managerId` le désigne.
>
> **Attention** : l'entité `Transaction` décrite plus bas existe en base, mais elle relève d'Atlantis SGI et
> n'est alimentée par aucun endpoint d'écriture de cette application (voir section 7.2). Tant qu'Atlantis
> n'est pas branché, les KPI de montants et de mandats resteront donc à zéro hors jeu de démonstration.

### 6.1. Objectif du module

Concevoir un modèle de données qui capture les **événements bruts** de l'activité commerciale (clients, rendez-vous, transactions), indépendamment de toute source (Atlantis SGI plus tard, saisie manuelle en attendant). Les KPI (nombre de clients, AUM, commissions, activité) sont **calculés à la demande** à partir de ces événements — ils ne sont pas stockés en dur, pour rester flexibles si les règles d'attribution ou de calcul évoluent.

### 6.2. Contexte métier à respecter

L'équipe commerciale actuelle est petite (2 commerciaux + 1 responsable). Ils prospectent souvent **ensemble** (en binôme, parfois avec le responsable présent pour les personnes morales), mais chaque commercial a **sa propre liste de clients à prospecter**. Il faut donc distinguer deux notions :

- **Le commercial référent** d'un client : celui à qui le client est rattaché dans sa liste. C'est lui qui reçoit le crédit du client dans les KPI individuels (nouveaux clients, AUM, commissions).
- **La participation aux rendez-vous** : qui était présent physiquement (souvent plusieurs personnes). Sert à mesurer l'activité terrain, pas la propriété du client. Sans cette distinction, le responsable qui accompagne plusieurs rendez-vous verrait ses propres KPI gonflés artificiellement.

### 6.3. Modèle de données

**Entité `Client`**
- `id`
- `type` (`PERSONNE_PHYSIQUE` ou `PERSONNE_MORALE`)
- `nom` / `raisonSociale`
- `commercialReferentId` (FK vers `User` — le commercial "propriétaire" du client pour les KPI)
- `dateAcquisition` (date à laquelle le client est devenu client, sert au KPI "nouveaux clients par période")
- `statut` (`PROSPECT`, `CLIENT_ACTIF`, `CLIENT_INACTIF`)
- `createdAt`, `updatedAt`

**Entité `RendezVous`**
- `id`
- `clientId` (FK vers `Client`)
- `date`
- `type` (`PROSPECTION`, `SUIVI`, `SIGNATURE`, ...)
- `compteRendu` (texte libre — le rapport fait au responsable)
- `createdAt`

**Table de liaison `rendez_vous_participants`** (many-to-many entre `RendezVous` et `User`)
- `rendezVousId`
- `userId` (peut être un commercial ou le responsable)

**Entité `Transaction`** (représente un événement financier générateur de KPI — collecte d'actif, signature de mandat, commission facturée)
- `id`
- `clientId` (FK vers `Client`)
- `type` (`INTERMEDIATION`, `GESTION_SOUS_MANDAT`)
- `montant` (decimal — en XOF/FCFA, devise unique pour l'instant)
- `dateTransaction`
- `description`
- `sourceSysteme` (`MANUEL`, `ATLANTIS` — enum extensible ; permet de tracer plus tard quelles données viennent d'où)
- `createdAt`

### 6.4. KPI calculés à partir de ce modèle (requêtes/vues, pas de stockage dur)

| KPI | Calcul |
|---|---|
| Nouveaux clients par commercial (période) | `COUNT(Client)` où `commercialReferentId = X` et `dateAcquisition` dans la période |
| AUM / montants collectés par commercial | `SUM(Transaction.montant)` des transactions liées aux clients dont `commercialReferentId = X` |
| Nombre de mandats de gestion signés | `COUNT(Transaction)` où `type = GESTION_SOUS_MANDAT` |
| Activité terrain (nombre de rendez-vous) | `COUNT` via `rendez_vous_participants` où `userId = X` — valorise l'effort même sans signature |
| Vue managériale | Agrégation sur tous les commerciaux, sans que la présence du responsable aux rendez-vous ne pollue les KPI individuels |

### 6.5. Points d'attention pour Claude Code

- Ne pas créer de FK directe entre `RendezVous` et un seul `User` (le commercial) — toujours passer par la table de liaison `rendez_vous_participants`, car plusieurs personnes peuvent être présentes.
- Le champ `commercialReferentId` sur `Client` reste **unique** (un seul référent par client) — c'est volontaire, voir section 6.2. Ne pas le transformer en relation many-to-many.
- `Transaction.montant` : utiliser un type `DECIMAL(18,2)` (ou équivalent T-SQL) — jamais de `FLOAT`/`DOUBLE` pour des montants financiers.
- Le champ `sourceSysteme` sur `Transaction` est là pour anticiper l'intégration Atlantis : garder cette entité **ouverte à l'ajout d'un `sourceReferenceId`** (identifiant de la transaction dans Atlantis) dans une migration future, sans avoir à tout redéfinir.
- Ce module s'appuie sur l'entité `User` déjà créée dans le module Utilisateurs/Rôles/Permissions — ne pas dupliquer une notion de "commercial" ailleurs.
- Devise : hypothèse posée de XOF/FCFA unique. Si multi-devise s'avère nécessaire plus tard, ajouter un champ `devise` sur `Transaction` — ne pas le faire maintenant (sur-ingénierie non demandée).
- Respecter les mêmes conventions déjà en place : package par fonctionnalité (`client/`, `rendezvous/`, `transaction/`), migrations Flyway en T-SQL, dialecte SQL Server.

---

## 7. Module livré (référence) : Gestion du portefeuille commercial (clients et rendez-vous)

> **État** : livré. CRUD paginé sur `/api/clients` et `/api/rendez-vous`, cloisonnement par référent
> appliqué en couche service (`PortfolioAccess`) en plus de `@PreAuthorize`, permissions
> `MANAGE_OWN_PORTFOLIO` / `MANAGE_ALL_PORTFOLIOS` / `DELETE_PORTFOLIO_DATA` (migration `V6`), traçabilité
> `created_by`/`updated_by` (migration `V7`). Aucun endpoint d'écriture sur les transactions.

### 7.1. Objectif du module

Donner à l'équipe commerciale une interface pour gérer **son portefeuille de prospects et de clients** et
**ses rendez-vous**. Ce sont des données dont l'application est la **seule source** : elles n'existent dans
aucun autre système de l'entreprise.

Ce module ne disparaîtra pas une fois Atlantis branché. Les prospects, les rendez-vous et les comptes rendus
sont propres au travail commercial et resteront saisis ici.

### 7.2. Frontière avec Atlantis SGI — à respecter absolument

La base de clients de l'équipe commerciale et la base de clients de l'entreprise **ne sont pas la même
chose** :

- La **base commerciale** (celle de cette application) contient à la fois des **prospects** encore
  démarchés et des **clients effectifs**. Chaque commercial y ajoute lui-même les personnes qu'il
  prospecte.
- La **base Atlantis** ne contient que les **clients effectifs**, ceux qui ont déjà souscrit à un service
  de la structure, ainsi que **toutes leurs opérations**.

La base commerciale est donc un **sur-ensemble** de la base Atlantis, et le champ `statut` du `Client`
(`PROSPECT`, `CLIENT_ACTIF`, `CLIENT_INACTIF`) est exactement ce qui distingue les deux populations.

Il en découle un partage des responsabilités strict :

| Donnée | Qui en est responsable |
|---|---|
| Clients et prospects | L'équipe commerciale, dans cette application |
| Rendez-vous, participants, comptes rendus | L'équipe commerciale, dans cette application |
| **Transactions et opérations financières** | **Atlantis SGI, jamais cette application** |

> **Règle non négociable** : l'application n'expose **aucune création, modification ou suppression de
> transaction**, pour personne — pas même pour le responsable commercial, qui n'a aucun pouvoir sur les
> opérations d'un client. Les transactions sont produites et gérées dans Atlantis. Une fois Atlantis pris en
> main, on cherchera à exploiter une API permettant d'en **lire** la liste pour un client donné, en
> consultation seule.

L'entité `Transaction` du module 2 reste donc en place, mais elle n'est alimentée par **aucun endpoint
d'écriture** : elle attend l'intégration Atlantis. Les transactions présentes dans `db/demo` sont un simple
jeu d'essai de développement, pas une fonctionnalité.

**Conséquence sur les KPI, à assumer** : tant qu'Atlantis n'est pas branché, les indicateurs de montants
collectés et de mandats signés resteront à zéro. Seuls « nouveaux clients par commercial » et « activité
terrain » seront réellement exploitables. Si l'entreprise a besoin des montants avant l'intégration, la
réponse ne sera pas une saisie manuelle de transactions par les commerciaux, mais un **import** depuis le
module Reporting d'Atlantis — à décider quand ses modalités d'accès seront connues.

### 7.3. Règle d'accès retenue : chacun gère son périmètre

- Un **commercial** crée et modifie les clients dont il est le référent, ainsi que les rendez-vous
  rattachés à ces clients. Il ne voit ni ne modifie le portefeuille de ses collègues.
- Le **responsable commercial** et la **direction** gèrent et corrigent pour toute l'équipe.
- Ce cloisonnement est une **règle métier vérifiée dans la couche service**, en plus du contrôle de
  permission fait par Spring Security. Une permission dit *ce qu'on a le droit de faire*, le cloisonnement
  dit *sur quelles lignes* — les deux sont nécessaires et ne se remplacent pas.
- Comme partout ailleurs dans le projet, le périmètre se déduit d'une **permission**, jamais d'un test sur
  le nom du rôle (`if role == COMMERCIAL` est proscrit).

### 7.4. Nouvelles permissions à créer (migration Flyway)

```
MANAGE_OWN_PORTFOLIO   — gérer ses propres clients/prospects et leurs rendez-vous
MANAGE_ALL_PORTFOLIOS  — gérer ceux de tous les commerciaux
DELETE_PORTFOLIO_DATA  — supprimer une saisie erronée (plutôt que la corriger)
```

Aucune permission d'écriture sur les transactions n'est créée, ni maintenant ni plus tard : voir 7.2.

Attribution proposée : `COMMERCIAL` → `MANAGE_OWN_PORTFOLIO` ; `MANAGER_COMMERCIAL` et `DIRECTION` →
`MANAGE_ALL_PORTFOLIOS` ; `ADMIN` → les trois.

> **Attention** : le seed `V3` a donné toutes les permissions à `ADMIN` via un `CROSS JOIN` exécuté une
> seule fois. Les permissions ajoutées maintenant **ne lui seront pas rattachées automatiquement** : la
> nouvelle migration doit explicitement les insérer dans `role_permissions` pour `ADMIN`, sinon
> l'administrateur se retrouvera sans droit de saisie.

### 7.5. Endpoints attendus

| Ressource | Endpoints |
|---|---|
| Clients | `GET /api/clients` (liste paginée, filtres : référent, statut, type, recherche par nom), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Rendez-vous | `GET /api/rendez-vous` (filtres : client, période, participant), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |

**Aucun endpoint `/api/transactions` n'est à créer dans ce module**, pas même en lecture : la source
n'existe pas encore. Il viendra avec l'intégration Atlantis, et en consultation seule.

Les listes sont **paginées** (`Pageable`) : contrairement aux utilisateurs, ces tables grossiront
continuellement et une liste complète deviendrait vite impraticable.

### 7.6. Règles de validation à faire respecter

- **Désignation** : une personne physique porte un `nom` et pas de `raisonSociale`, une personne morale
  l'inverse. La contrainte `CK_clients_designation` existe déjà en base, mais l'API doit renvoyer une erreur
  de validation lisible plutôt que laisser remonter une violation SQL.
- **`dateAcquisition`** ne peut pas être dans le futur. Un **rendez-vous**, en revanche, peut être daté dans
  le futur : l'équipe planifie ses visites.
- **Participants** : au moins un participant par rendez-vous, sinon l'activité terrain n'est imputée à
  personne.
- **Changement de référent** : réservé à `MANAGE_ALL_PORTFOLIOS`. Réaffecter un client déplace ses clients
  et ses montants d'un commercial à un autre dans les KPI — ce n'est pas une correction anodine.
- **Suppression** : interdite sur un client qui porte des transactions, puisque celles-ci proviennent
  d'Atlantis et que l'application n'a pas à faire disparaître un client effectif. Passer son `statut` à
  `CLIENT_INACTIF` est la bonne réponse. La suppression reste possible sur un prospect sans historique et
  sur un rendez-vous isolé, avec `DELETE_PORTFOLIO_DATA`.

### 7.7. Traçabilité de la saisie

Ajouter `created_by` et `updated_by` (clés étrangères vers `users`) sur `clients` et `rendez_vous`, via
migration. Ces données étant saisies à la main et servant à évaluer les personnes qui les saisissent, il faut
pouvoir répondre à « qui a enregistré ce client, et quand ». Ces colonnes sont renseignées à partir de
l'utilisateur authentifié, jamais depuis le corps de la requête. La table `transactions` n'est pas concernée :
l'application ne l'écrit pas.

### 7.8. Points d'attention pour Claude Code

- **Ne jamais exposer d'écriture sur les transactions**, quelle que soit la permission de l'appelant
  (voir 7.2). C'est la contrainte la plus importante de ce module.
- Aucun KPI n'est à recalculer ni à invalider après une saisie : ils sont calculés à la demande à partir des
  tables. C'est précisément l'intérêt du choix fait au module 2.
- Réutiliser les entités existantes du module 2 **sans les dupliquer** ni créer de nouvelles tables pour les
  mêmes concepts.
- Respecter les conventions en place : packages par fonctionnalité, DTO de requête/réponse séparés des
  entités, migrations Flyway en T-SQL, `@PreAuthorize` sur permission.
- Prévoir des tests sur le cloisonnement (un commercial ne peut pas modifier le client d'un collègue) et sur
  les règles de validation — ce sont les endroits où une régression passerait inaperçue.
- **Évolution anticipée, à ne pas construire maintenant** : quand un prospect devient client effectif, il
  existe aussi dans Atlantis. Il faudra alors une référence vers son identifiant Atlantis sur `Client` pour
  rapprocher les deux bases. Ce champ s'ajoutera par migration le moment venu, sans remettre en cause le
  modèle.
- **Frontend** : écrans sous `app/(commercial)/clients/` et `app/(commercial)/rendez-vous/`, avec les
  actions masquées selon les permissions de l'utilisateur connecté, comme pour les écrans d'administration.
  Aucun écran de saisie de transaction.

---

## 8. Module livré (référence) : Objectifs commerciaux

> **État** : livré. Objectifs individuels ou d'équipe sur `/api/objectifs`, avec taux d'atteinte recalculé
> à la demande via `KpiService` (jamais stocké). Introduit `manager_id` sur `User` (migration `V8`) et la
> table `objectifs` (migration `V9`), ainsi que la permission `MANAGE_ALL_OBJECTIVES` (migration `V10`).

### 8.1. Objectif du module

Permettre à un responsable ou à la direction de **fixer une cible** sur l'une des métriques déjà calculées
par le module KPI (nouveaux clients, montant collecté, mandats signés, activité terrain), sur une période
donnée, pour un commercial ou pour toute une équipe — puis de suivre le **taux d'atteinte** de cette cible.
Comme pour les KPI, la valeur réelle et le taux d'atteinte ne sont jamais stockés : ils sont recalculés à
chaque lecture à partir des mêmes tables (`clients`, `transactions`, `rendez_vous_participants`), pour ne
jamais avoir deux façons de compter la même chose.

### 8.2. Lien manager → équipe (nouveau sur `User`)

Le module KPI (section 6) avait signalé que `VIEW_TEAM_DASHBOARD` était inexploitable faute de notion
d'équipe dans le modèle. Ce module tranche la question : `User` reçoit un champ `managerId` (FK vers
`User`, nullable). L'équipe d'un responsable est l'ensemble des commerciaux dont `managerId` le désigne.
Un utilisateur sans responsable assigné (`managerId` nul) n'appartient à l'équipe de personne : seule la
direction (`MANAGE_ALL_OBJECTIVES`) peut alors agir pour lui.

### 8.3. Modèle de données

**Entité `Objectif`**
- `id`
- `type` (`NOUVEAUX_CLIENTS`, `MONTANT_COLLECTE`, `MANDATS_SIGNES`, `ACTIVITE_TERRAIN`) — une métrique
  parmi celles déjà calculées par le module KPI, jamais une mesure inédite
- `commercialId` (FK vers `User`, nullable) — renseigné pour un objectif **individuel**
- `managerId` (FK vers `User`, nullable) — renseigné pour un objectif **d'équipe** ; c'est une cible
  propre que le responsable fixe pour son équipe, **pas** la somme des objectifs individuels de ses
  commerciaux : les deux existent indépendamment l'un de l'autre
- Exactement un des deux titulaires est renseigné (contrainte `CK_objectifs_titulaire`, revalidée côté
  service avec un message lisible)
- `valeurCible` (`DECIMAL(18,2)`, strictement positive)
- `dateDebut`, `dateFin` (période libre, propre à chaque objectif ; `dateFin >= dateDebut`)
- `createdBy`, `updatedBy`, `createdAt`, `updatedAt`

### 8.4. Règle d'accès : cloisonnement par équipe

- `MANAGE_OBJECTIVES` (déjà seedée au module 1) autorise à fixer des objectifs, mais **seulement pour sa
  propre équipe** : un objectif individuel doit viser un commercial dont `managerId` est l'appelant, un
  objectif d'équipe doit viser l'équipe de l'appelant lui-même.
- `MANAGE_ALL_OBJECTIVES` (nouvelle permission) étend ce périmètre à tous les commerciaux et équipes,
  sur le même principe que `MANAGE_ALL_PORTFOLIOS` pour le portefeuille. Attribuée à `ADMIN` et
  `DIRECTION`.
- Ce cloisonnement est vérifié en couche service (`ObjectifAccess`), y compris lors d'une **modification** :
  l'accès est revérifié sur l'affectation *actuelle* de l'objectif avant d'appliquer la nouvelle, pour
  qu'un responsable ne puisse pas s'approprier l'objectif d'une autre équipe en le réaffectant à la sienne.
- Comme partout ailleurs, le périmètre se déduit d'une **permission**, jamais d'un nom de rôle.

### 8.5. Endpoints livrés

| Endpoint | Permission | Description |
|---|---|---|
| `GET /api/objectifs/me` | `VIEW_OWN_DASHBOARD` | Objectifs individuels de l'utilisateur connecté |
| `GET /api/objectifs/equipe` | `VIEW_TEAM_DASHBOARD` | Objectif(s) d'équipe du responsable connecté + objectifs individuels de ses commerciaux |
| `GET /api/objectifs` | `VIEW_ALL_DASHBOARDS` | Vue consolidée paginée (filtres : commercial, équipe, type) |
| `POST /api/objectifs` | `MANAGE_OBJECTIVES` ou `MANAGE_ALL_OBJECTIVES` | Fixer un objectif |
| `PUT /api/objectifs/{id}` | `MANAGE_OBJECTIVES` ou `MANAGE_ALL_OBJECTIVES` | Modifier un objectif |
| `DELETE /api/objectifs/{id}` | `MANAGE_OBJECTIVES` ou `MANAGE_ALL_OBJECTIVES` | Supprimer un objectif |

### 8.6. Points d'attention pour Claude Code

- La valeur réelle et le taux d'atteinte ne sont calculés qu'au moment de la lecture, via `KpiService`
  (méthode `forUser`) — ne jamais les stocker sur l'entité `Objectif`, ni les invalider/recalculer après
  une saisie sur `clients`, `transactions` ou `rendez_vous`.
- Un objectif d'équipe agrège les valeurs individuelles de ses membres (`findByManagerId`), mais reste une
  cible **propre** : ne pas en déduire automatiquement un objectif d'équipe à partir des objectifs
  individuels, ni l'inverse.
- `CurrentUser` (dans `common/security`) centralise l'accès à l'utilisateur authentifié et à ses
  autorités ; `PortfolioAccess` et `ObjectifAccess` s'appuient dessus plutôt que de dupliquer cette
  logique — réutiliser ce composant pour tout futur cloisonnement de ce type.
- **Frontend** : écrans sous `app/(commercial)/objectifs/`, avec un formulaire de fixation qui bascule
  entre « pour un commercial » et « pour une équipe » plutôt que d'exposer les deux champs en même temps.
  Le taux d'atteinte doit être visible à la fois sur la vue individuelle et sur la vue d'équipe.
- **Évolution anticipée, déjà réalisée** : `VIEW_TEAM_DASHBOARD` s'appuie maintenant sur `managerId` via
  `GET /api/kpi/equipe`, ajouté par le module Tableaux de bord (section 9).

---

## 9. Module livré (référence) : Tableaux de bord

> **État** : livré côté backend, avec un périmètre volontairement réduit — voir 9.1.

### 9.1. Portée retenue

Les KPI (module 2) et les Objectifs (module 4) exposaient déjà des endpoints granulaires pour les trois
vues attendues (individuelle, équipe, globale), sauf un : `GET /api/kpi/equipe`. Plutôt que de construire un
nouveau point d'entrée consolidé qui agrégerait KPI et Objectifs en un seul appel (une forme de réponse
supplémentaire à maintenir en double avec celles des deux modules), le choix retenu a été de **combler
uniquement ce trou** et de laisser le frontend composer ses écrans de tableau de bord à partir des
endpoints déjà livrés :

| Vue | KPI | Objectifs |
|---|---|---|
| Individuelle | `GET /api/kpi/me` (`VIEW_OWN_DASHBOARD`) | `GET /api/objectifs/me` (`VIEW_OWN_DASHBOARD`) |
| Équipe | `GET /api/kpi/equipe` (`VIEW_TEAM_DASHBOARD`, **nouveau**) | `GET /api/objectifs/equipe` (`VIEW_TEAM_DASHBOARD`) |
| Globale (direction) | `GET /api/kpi/commerciaux` (`VIEW_ALL_DASHBOARDS`) | `GET /api/objectifs` (`VIEW_ALL_DASHBOARDS`) |

### 9.2. `GET /api/kpi/equipe`

Sur le même modèle que `GET /api/kpi/commerciaux` (vue direction), mais le périmètre se déduit du lien
`managerId` de l'appelant plutôt que d'une permission de suivi individuel : l'équipe d'un responsable est
l'ensemble des commerciaux **actifs** dont `managerId` le désigne (`UserRepository.findByManagerIdAndActiveTrue`).
Renvoie la même forme que `KpiPeriodeResponse` (détail par commercial + totaux d'équipe).

### 9.3. Points d'attention pour Claude Code

- Ce périmètre réduit est un choix délibéré, pas un oubli : ne pas ajouter de module `dashboard/` ni de
  DTO consolidé sans qu'un besoin explicite ne le justifie (par exemple si le frontend se retrouve à
  dupliquer une logique d'agrégation qui devrait vivre côté backend).
- `forTeam` filtre sur les membres **actifs** de l'équipe, comme `forAllCommerciaux` filtre déjà les
  comptes actifs pour la vue direction — un commercial désactivé n'apparaît plus dans les tableaux de bord
  courants, même si son historique reste compté dans les périodes passées auxquelles il a contribué.
- Le frontend (hors périmètre de ce backend) doit composer ses écrans à partir des deux appels (KPI +
  Objectifs) par vue, comme indiqué dans le tableau ci-dessus.

---

## 10. Ce qui n'est PAS à faire maintenant

- Ne pas intégrer Atlantis SGI (en attente d'informations)
- **Ne créer aucun endpoint ni écran d'écriture sur les transactions** : elles appartiennent à Atlantis
- Ne pas créer de module `dashboard/` ni d'endpoint consolidé KPI + Objectifs : choix explicite, voir 9.1
- Ne pas modifier le modèle de données des modules 2, 3 et 4 : les tableaux de bord doivent s'y adapter, pas l'inverse
- Rester concentré sur l'endpoint KPI d'équipe manquant
