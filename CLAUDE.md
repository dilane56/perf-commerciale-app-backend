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

1. ✅ **Module en cours : Utilisateurs, Rôles & Permissions** (objet de ce document)
2. Modèle de données KPI générique (indépendant de la source)
3. Module de saisie manuelle temporaire (en attendant l'intégration Atlantis)
4. Module Objectifs commerciaux (fixation + suivi du taux d'atteinte)
5. Tableaux de bord (vue individuelle / vue managériale)
6. Intégration Atlantis SGI (une fois les modalités d'accès connues)

Ce fichier se concentre uniquement sur le **point 1**.

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

## 5. Module actuel : Gestion des Utilisateurs, Rôles & Permissions (RBAC)

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

## 6. Ce qui n'est PAS à faire maintenant

- Ne pas intégrer Atlantis SGI (en attente d'informations)
- Ne pas construire les modules KPI, objectifs ou dashboards (viendront après)
- Rester concentré sur l'authentification et la gestion des utilisateurs/rôles/permissions comme fondation du projet
