# Product Backlog — Kanban App

> **Projet PFE** | Méthodologie Agile Scrum

---

## Définition des priorités
| Priorité | Description |
|----------|-------------|
| 🔴 Must Have | Fonctionnalité indispensable |
| 🟠 Should Have | Importante mais pas bloquante |
| 🟡 Nice to Have | Amélioration optionnelle |

---

## User Stories

### Epic 1 — Authentification

| ID | User Story | Priorité | Points |
|----|-----------|----------|--------|
| US-01 | En tant qu'**utilisateur**, je veux **m'inscrire** avec mon nom, email et mot de passe, afin de **créer mon compte** sur l'application. | 🔴 | 3 |
| US-02 | En tant qu'**utilisateur**, je veux **me connecter** avec mon email et mot de passe, afin d'**accéder à mes boards**. | 🔴 | 2 |

**Critères d'acceptation US-01 :**
- Email unique obligatoire
- Mot de passe minimum 8 caractères
- Email de bienvenue envoyé automatiquement
- Redirection vers le dashboard après inscription

**Critères d'acceptation US-02 :**
- Message d'erreur si identifiants incorrects
- Session maintenue pendant toute la durée de connexion

---

### Epic 2 — Gestion des Boards

| ID | User Story | Priorité | Points |
|----|-----------|----------|--------|
| US-03 | En tant qu'**utilisateur**, je veux **créer un board** avec un titre, une couleur et une visibilité, afin d'**organiser mon projet**. | 🔴 | 3 |
| US-04 | En tant qu'**admin board**, je veux **inviter des membres** par email, afin de **collaborer avec mon équipe**. | 🔴 | 3 |
| US-12 | En tant qu'**admin board**, je veux **supprimer un board**, afin de **clôturer un projet terminé**. | 🟠 | 2 |

**Critères d'acceptation US-03 :**
- 4 colonnes créées automatiquement (À faire, En cours, En révision, Terminé)
- Couleur de thème personnalisable
- Visibilité PUBLIC ou PRIVATE

**Critères d'acceptation US-04 :**
- Vérification que l'email existe dans la base
- Impossibilité d'inviter un membre déjà présent
- Email d'invitation envoyé automatiquement

**Critères d'acceptation US-12 :**
- Confirmation avant suppression
- Suppression en cascade (colonnes + tâches + commentaires)
- Email de notification envoyé à tous les membres

---

### Epic 3 — Gestion des Tâches

| ID | User Story | Priorité | Points |
|----|-----------|----------|--------|
| US-05 | En tant qu'**utilisateur**, je veux **créer une tâche** avec titre, description, priorité, deadline et assignation, afin de **définir le travail à faire**. | 🔴 | 5 |
| US-06 | En tant qu'**utilisateur**, je veux **modifier une tâche**, afin de **mettre à jour son contenu**. | 🔴 | 3 |
| US-07 | En tant qu'**utilisateur**, je veux **supprimer une tâche**, afin de **retirer du travail annulé**. | 🔴 | 2 |
| US-08 | En tant qu'**utilisateur**, je veux **déplacer une tâche** entre colonnes par glisser-déposer, afin de **mettre à jour son statut**. | 🔴 | 5 |

**Critères d'acceptation US-05 :**
- Priorité : HIGH, MEDIUM, LOW
- Format deadline : YYYY-MM-DD
- Email d'assignation envoyé si un utilisateur est assigné
- Tâche visible immédiatement dans la colonne

**Critères d'acceptation US-08 :**
- Limite WIP respectée (erreur si dépassée)
- Email envoyé au créateur si tâche déplacée en "Terminé"
- Broadcast temps réel vers tous les membres connectés

---

### Epic 4 — Collaboration

| ID | User Story | Priorité | Points |
|----|-----------|----------|--------|
| US-09 | En tant qu'**utilisateur**, je veux **commenter une tâche**, afin de **communiquer avec mon équipe**. | 🟠 | 3 |
| US-11 | En tant qu'**utilisateur**, je veux **voir les mises à jour en temps réel**, afin d'**être synchronisé avec mon équipe**. | 🔴 | 8 |

**Critères d'acceptation US-09 :**
- Commentaire affiché avec auteur et date
- Email de notification au créateur de la tâche

**Critères d'acceptation US-11 :**
- Mise à jour automatique sans rechargement manuel
- Toast notification pour chaque événement reçu

---

### Epic 5 — Notifications

| ID | User Story | Priorité | Points |
|----|-----------|----------|--------|
| US-10 | En tant qu'**utilisateur**, je veux **recevoir des emails** pour les événements importants, afin de **rester informé même hors connexion**. | 🟠 | 5 |

**Critères d'acceptation US-10 :**
- Email de bienvenue à l'inscription
- Email d'invitation board
- Email d'assignation de tâche
- Email de rappel deadline (J-1, à minuit)
- Email quand une tâche est terminée
- Email de suppression de board

---

## Récapitulatif

| Epic | User Stories | Points Total |
|------|-------------|-------------|
| Authentification | US-01, US-02 | 5 |
| Boards | US-03, US-04, US-12 | 8 |
| Tâches | US-05, US-06, US-07, US-08 | 15 |
| Collaboration | US-09, US-11 | 11 |
| Notifications | US-10 | 5 |
| **Total** | **12 US** | **44 points** |