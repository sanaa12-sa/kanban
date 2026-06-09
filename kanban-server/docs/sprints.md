# Planification des Sprints — Kanban App

> **Durée de chaque sprint :** 2 semaines  
> **Méthodologie :** Agile Scrum  
> **Équipe :** 4 développeurs

---

## Vue d'ensemble

```
Sprint 1  ──────────────────►  Authentification + Boards
Sprint 2  ──────────────────►  Tâches CRUD
Sprint 3  ──────────────────►  Collaboration + Notifications
Sprint 4  ──────────────────►  Temps réel + DevOps + Tests
```

---

## Sprint 1 — Fondations

**Objectif :** Mettre en place la base du projet et l'authentification

**User Stories :**
| ID | Description | Points | Statut |
|----|-------------|--------|--------|
| US-01 | Register | 3 | ✅ Done |
| US-02 | Login | 2 | ✅ Done |
| US-03 | Créer board + colonnes par défaut | 3 | ✅ Done |

**Tâches techniques :**
- [x] Mise en place du projet Maven (serveur + client)
- [x] Configuration Hibernate + MySQL
- [x] Modèles JPA : User, Board, BoardColumn
- [x] Server TCP + ClientHandler + SessionManager
- [x] Interface JavaFX : Welcome, Login, Register, Dashboard
- [x] Hachage mot de passe SHA-256

**Total points :** 8  
**Vélocité équipe :** 8 points/sprint

---

## Sprint 2 — Gestion des Tâches

**Objectif :** Implémenter toutes les opérations CRUD sur les tâches

**User Stories :**
| ID | Description | Points | Statut |
|----|-------------|--------|--------|
| US-04 | Inviter des membres | 3 | ✅ Done |
| US-05 | Créer une tâche | 5 | ✅ Done |
| US-06 | Modifier une tâche | 3 | ✅ Done |
| US-07 | Supprimer une tâche | 2 | ✅ Done |

**Tâches techniques :**
- [x] Modèles JPA : Task, Comment, BoardMember
- [x] TaskService : createTask, updateTask, deleteTask
- [x] BoardService : inviteMember, getMembers
- [x] Interface JavaFX : KanbanBoard, CreateTask, EditTask
- [x] Intégration Design Pattern Builder (Task, Board, User)
- [x] RequestHandler : nouvelles actions

**Total points :** 13

---

## Sprint 3 — Collaboration et Notifications

**Objectif :** Ajouter les commentaires et les notifications email

**User Stories :**
| ID | Description | Points | Statut |
|----|-------------|--------|--------|
| US-08 | Drag & Drop avec WIP limit | 5 | ✅ Done |
| US-09 | Commentaires sur les tâches | 3 | ✅ Done |
| US-10 | Notifications email | 5 | ✅ Done |
| US-12 | Supprimer un board | 2 | ✅ Done |

**Tâches techniques :**
- [x] Drag & Drop JavaFX avec vérification WIP
- [x] TaskService : addComment, getComments
- [x] EmailNotificationService : 7 types d'emails
- [x] EmailTemplates HTML (dark mode)
- [x] DeadlineScheduler (vérification quotidienne à minuit)
- [x] Interface JavaFX : Comments, InviteMember
- [x] Intégration Design Pattern Adapter (NotificationSender)
- [x] Intégration Design Pattern Composite (Board → Column → Task)

**Total points :** 15

---

## Sprint 4 — Qualité et DevOps

**Objectif :** Finir le temps réel, les tests et la mise en production

**User Stories :**
| ID | Description | Points | Statut |
|----|-------------|--------|--------|
| US-11 | Temps réel (broadcast TCP) | 8 | ✅ Done |

**Tâches techniques :**
| Tâche | Responsable | Statut |
|-------|-------------|--------|
| ServerListener + BoardBroadcaster | Équipe | ✅ Done |
| Tests JUnit 5 (UserService, BoardService, TaskService) | P1 | 🔄 En cours |
| Tests Design Patterns | P1 | 🔄 En cours |
| Dockerfile + docker-compose | P2 | 🔄 En cours |
| GitHub Actions CI/CD | P3 | 🔄 En cours |
| README.md | P3 | 🔄 En cours |
| specifications.md | P4 | ✅ Done |
| backlog.md | P4 | ✅ Done |
| sprints.md | P4 | ✅ Done |
| logback.xml | P4 | ✅ Done |
| prometheus.yml | P4 | ✅ Done |

**Total points :** 8 + tâches DevOps

---

## Récapitulatif des Sprints

| Sprint | Points | Durée | Focus |
|--------|--------|-------|-------|
| Sprint 1 | 8 | 2 semaines | Authentification + Boards |
| Sprint 2 | 13 | 2 semaines | CRUD Tâches |
| Sprint 3 | 15 | 2 semaines | Collaboration + Emails |
| Sprint 4 | 8+ | 2 semaines | Temps réel + DevOps |
| **Total** | **44** | **8 semaines** | |

---

## Définition of Done (DoD)

Une User Story est considérée **terminée** quand :
- ✅ Le code est implémenté et fonctionne
- ✅ Le code est poussé sur la branche `develop`
- ✅ Les tests unitaires sont écrits et passent
- ✅ La fonctionnalité est testée manuellement
- ✅ Le code est revu par un autre membre de l'équipe
- ✅ Aucune régression sur les fonctionnalités existantes