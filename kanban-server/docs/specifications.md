# Spécifications Fonctionnelles et Techniques — Kanban App

> **Projet** | Architecture Client-Serveur | Java 21 + JavaFX + Hibernate + MySQL

---

## 1. Description Générale

L'application **Kanban App** est une plateforme collaborative de gestion de projets permettant aux équipes d'organiser leur travail sous forme de boards, colonnes et tâches.

### Objectifs principaux
- Permettre à chaque utilisateur de créer et gérer des **boards Kanban**
- Faciliter la **collaboration en équipe** via des invitations et des commentaires
- Assurer une **synchronisation en temps réel** entre tous les membres connectés
- Envoyer des **notifications email** automatiques pour les événements importants

### Fonctionnalités disponibles
- Créer des boards avec colonnes personnalisables
- Inviter des membres et gérer les rôles
- Créer, modifier, supprimer et déplacer des tâches (drag & drop)
- Commenter les tâches
- Recevoir des notifications email (assignation, deadline, commentaire, suppression)
- Voir les mises à jour en temps réel via broadcast TCP

---

## 2. Acteurs

### Utilisateur (rôle MEMBER)
| Action | Description |
|--------|-------------|
| S'inscrire | Créer un compte avec nom, email, mot de passe |
| Se connecter | Authentification par email + mot de passe |
| Créer un board | Nouveau tableau avec titre, couleur, visibilité |
| Gérer les tâches | Créer, modifier, supprimer, déplacer |
| Commenter | Ajouter des commentaires sur les tâches |
| Voir les boards | Accéder aux boards dont il est membre |

### Admin Board (rôle ADMIN / Créateur)
| Action | Description |
|--------|-------------|
| Inviter des membres | Ajouter un utilisateur par email |
| Supprimer le board | Suppression en cascade (tâches + commentaires) |
| Superviser les tâches | Voir toutes les tâches de tous les membres |
| Recevoir notifications | Email lors de suppression ou completion |

---

## 3. Fonctionnalités Détaillées

### 3.1 Authentification
- **Register** : validation email unique, mot de passe ≥ 8 caractères, hachage SHA-256, email de bienvenue automatique
- **Login** : vérification email + mot de passe hashé, création de session TCP

### 3.2 Gestion des Boards
- **Créer board** : titre, description, couleur thème, visibilité (PUBLIC/PRIVATE), 4 colonnes par défaut (À faire, En cours, En révision, Terminé)
- **Lister boards** : boards créés + boards dont l'utilisateur est membre
- **Supprimer board** : suppression en cascade + notification email à tous les membres

### 3.3 Gestion des Tâches
- **Créer tâche** : titre, description, priorité (HIGH/MEDIUM/LOW), deadline, assignation, email automatique à l'assigné
- **Modifier tâche** : mise à jour de tous les champs
- **Supprimer tâche** : suppression avec ses commentaires liés
- **Déplacer tâche** : drag & drop entre colonnes, vérification limite WIP, email si tâche terminée
- **Rechercher tâches** : recherche par titre ou description

### 3.4 Collaboration
- **Commentaires** : ajout de commentaires, notification email au créateur de la tâche
- **Invitations** : invitation par email, vérification doublon, email d'invitation envoyé
- **Membres** : liste des membres avec leurs rôles

### 3.5 Notifications Email
| Événement | Destinataire |
|-----------|-------------|
| Inscription | Nouvel utilisateur |
| Invitation board | Utilisateur invité |
| Assignation tâche | Utilisateur assigné |
| Tâche terminée | Créateur de la tâche |
| Nouveau commentaire | Créateur de la tâche |
| Rappel deadline (J-1) | Utilisateur assigné |
| Suppression board | Tous les membres |

### 3.6 Temps Réel
- Broadcast TCP vers tous les clients connectés au même board
- Événements broadcastés : TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASK_MOVED, COMMENT_ADDED
- Toast notifications côté client JavaFX

---

## 4. Architecture Technique

```
┌─────────────────────────────────────────────┐
│           CLIENT (JavaFX)                    │
│  MainApp → Controllers → ServerConnection   │
│  ServerListener (broadcast temps réel)      │
└──────────────────┬──────────────────────────┘
                   │ TCP Socket (port 5000)
                   │ JSON (Gson)
┌──────────────────▼──────────────────────────┐
│           SERVEUR (Java TCP)                 │
│  Server → ClientHandler → RequestHandler    │
│  Services : UserService, BoardService,      │
│             TaskService                     │
│  SessionManager + BoardBroadcaster          │
└──────────────────┬──────────────────────────┘
                   │ Hibernate ORM
┌──────────────────▼──────────────────────────┐
│           BASE DE DONNÉES (MySQL)            │
│  users, boards, columns, tasks,             │
│  comments, board_members                    │
└─────────────────────────────────────────────┘
```

### Technologies utilisées
| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 | Langage principal |
| JavaFX | 21 | Interface graphique client |
| Hibernate ORM | 6.4.4 | Accès base de données |
| MySQL | 8.0 | Base de données |
| Maven | 3.9 | Gestion des dépendances |
| Gson | 2.10.1 | Sérialisation JSON |
| JavaMail | 2.0.1 | Envoi d'emails |
| Docker | latest | Conteneurisation |
| Prometheus | latest | Monitoring |
| Logback | 1.5.6 | Logging |

---

## 5. Design Patterns

### 5.1 Builder Pattern
**Où :** `Task.java`, `Board.java`, `User.java`

**Pourquoi :** Ces objets ont de nombreux attributs dont certains sont optionnels. Le Builder permet de construire ces objets de façon lisible et sécurisée, en évitant les constructeurs avec trop de paramètres.

```java
// Exemple d'utilisation
Task task = new Task.Builder(columnId, "Fix bug", userId)
    .description("Corriger le bug de connexion")
    .priority(Priority.HIGH)
    .deadline(LocalDate.of(2026, 6, 1))
    .build();
```

### 5.2 Composite Pattern
**Où :** `pattern/BoardComposite.java`, `pattern/ColumnComposite.java`, `pattern/TaskLeaf.java`, `pattern/BoardComponent.java`

**Pourquoi :** La structure Board → Column → Task est naturellement hiérarchique. Le Composite permet de traiter uniformément les nœuds et les feuilles via l'interface `BoardComponent`.

```
BoardComposite (Board)
  └── ColumnComposite (Colonne)
        └── TaskLeaf (Tâche)
```

### 5.3 Adapter Pattern
**Où :** `adapter/NotificationSender.java`, `adapter/EmailNotificationAdapter.java`

**Pourquoi :** Le code métier (Services) ne dépend que de l'interface `NotificationSender`. L'`EmailNotificationAdapter` adapte le service email existant vers cette interface. Si demain on veut envoyer des SMS ou des notifications Slack, on crée un nouvel Adapter sans toucher aux services.

```java
// Le service utilise l'interface, pas l'implémentation
NotificationSender notifier = new EmailNotificationAdapter();
notifier.sendWelcome(email, fullName);
```

---

## 6. Modèle de Données

```
users          : id, full_name, email, password_hash, profile_picture, created_at
boards         : id, title, description, theme_color, visibility, created_by, created_at
columns        : id, board_id, name, position, wip_limit
tasks          : id, column_id, title, description, priority, assigned_to, created_by, deadline, created_at
comments       : id, task_id, user_id, content, created_at
board_members  : id, board_id, user_id, role
```

---

## 7. Protocole de Communication

### Format des requêtes (Client → Serveur)
```json
{
  "action": "CREATE_TASK",
  "data": {
    "columnId": 1,
    "title": "Fix bug",
    "priority": "HIGH",
    "deadline": "2026-06-01",
    "assignedTo": 3
  }
}
```

### Format des réponses (Serveur → Client)
```json
{
  "status": "OK",
  "message": "Tâche créée",
  "data": { ... }
}
```

### Actions disponibles
| Action | Description |
|--------|-------------|
| REGISTER | Inscription |
| LOGIN | Connexion |
| CREATE_BOARD | Créer un board |
| GET_BOARDS | Lister les boards |
| DELETE_BOARD | Supprimer un board |
| INVITE_MEMBER | Inviter un membre |
| GET_MEMBERS | Lister les membres |
| CREATE_TASK | Créer une tâche |
| UPDATE_TASK | Modifier une tâche |
| DELETE_TASK | Supprimer une tâche |
| MOVE_TASK | Déplacer une tâche |
| GET_TASKS | Lister les tâches |
| SEARCH_TASKS | Rechercher des tâches |
| ADD_COMMENT | Ajouter un commentaire |
| GET_COMMENTS | Lister les commentaires |