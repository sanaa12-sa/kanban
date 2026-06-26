# Kanban App

Application Kanban collaborative en temps réel avec serveur Java TCP et client JavaFX.

## Architecture

```
Client JavaFX → TCP (port 5009) → Serveur Java → Hibernate → MySQL
```

## Lancement rapide avec Docker (recommandé)

> Seul Docker Desktop est nécessaire — aucune installation de Java ou MySQL requise.

### 1. Cloner le projet
```bash
git clone https://github.com/sanaa12-sa/kanban.git
cd kanban/kanban-server
```

### 2. Builder l'image du serveur
```bash
docker build -t kanban-server:latest .
```

### 3. Lancer tous les services
```bash
docker-compose up -d
```

Cela démarre automatiquement :

| Service | URL | Identifiants |
|---|---|---|
| Serveur Kanban (socket) | localhost:5009 | — |
| Métriques | http://localhost:8080/metrics | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| MySQL | localhost:3307 | root / root |

### 4. Vérifier que le serveur est prêt
```bash
docker logs kanban-server
```
Vous devez voir :
```
✅ Connexion Hibernate OK
🚀 Serveur Kanban démarré sur le port 5009
```

### 5. Lancer le client JavaFX
```bash
cd ../kanban-client
mvn clean javafx:run
```
Le client se connecte automatiquement sur `localhost:5009`.

### Arrêter l'application
```bash
docker-compose down
```

---

## Installation locale (sans Docker)

### Prérequis
- Java 17
- Maven 3.9+
- MySQL 8.0

### 1. Cloner le projet
```bash
git clone https://github.com/sanaa12-sa/kanban.git
cd kanban
```

### 2. Configurer MySQL
```sql
CREATE DATABASE kanban_db;
```
Modifier `kanban-server/src/main/resources/hibernate.cfg.xml` :
```xml
<property name="hibernate.connection.url">
  jdbc:mysql://localhost:3306/kanban_db
</property>
```

### 3. Lancer le serveur
```bash
cd kanban-server
mvn clean compile exec:java
```

### 4. Lancer le client
```bash
cd kanban-client
mvn clean javafx:run
```

---

## Docker — Commandes utiles

```bash
# Voir les logs en direct
docker-compose logs -f kanban-server

# Arrêter et supprimer les volumes
docker-compose down -v

# Rebuilder après modification du code
docker build --no-cache -t kanban-server:latest .
docker-compose down
docker-compose up -d
```

---

## Docker Swarm (Orchestration)

```bash
docker swarm init
docker stack deploy -c docker-compose.swarm.yml kanban
docker stack ps kanban
docker stack rm kanban
```

---

## Monitoring

### Prometheus
- URL : http://localhost:9090
- Config : `kanban-server/prometheus.yml`
- Métriques collectées toutes les 10 secondes

### Grafana
- URL : http://localhost:3000
- Login : `admin` / `admin`
- Dashboard : **Kanban Monitoring**
  1. Ouvrir http://localhost:3000
  2. Connections → Data sources → Prometheus → URL : `http://prometheus:9090`
  3. Dashboards → New → créer les panels `kanban_connections_total` et `kanban_active_connections`

### Métriques personnalisées exposées

| Métrique | Type | Description |
|---|---|---|
| `kanban_connections_total` | Counter | Nombre total de connexions depuis le démarrage |
| `kanban_active_connections` | Gauge | Nombre de clients actuellement connectés |

### Logging (Logback)
- Console + fichier : `logs/kanban.log`
- Rotation quotidienne, 30 jours de rétention
- Config : `kanban-server/src/main/resources/logback.xml`

---

## Tests

```bash
cd kanban-server
mvn test
mvn test -Dtest=UserServiceTest
mvn test -Dtest=BoardServiceTest
mvn test -Dtest=TaskServiceTest
mvn test -Dtest=CompositePatternTest
```

---

## CI/CD Pipeline

GitHub Actions sur chaque push `main` / `develop` :
1. **build-and-test** : compilation + 44 tests JUnit
2. **docker-build** : construction et test de l'image Docker
3. **deploy-staging** : déploiement automatique sur staging (`main` uniquement)

---

## Design Patterns

| Pattern | Classes | Rôle |
|---|---|---|
| Builder | `User`, `Board`, `Task` | Construction flexible des objets |
| Composite | `BoardComposite`, `ColumnComposite`, `TaskLeaf` | Hiérarchie Board > Colonne > Tâche |
| Adapter | `EmailNotificationAdapter` | Compatibilité entre modules |

---

## Structure du projet

```
kanban/
├── .github/
│   └── workflows/ci.yml
├── README.md
├── kanban-server/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── docker-compose.swarm.yml
│   ├── prometheus.yml
│   ├── grafana/
│   │   ├── datasources/prometheus.yml
│   │   └── dashboards/kanban-dashboard.json
│   ├── docs/
│   │   ├── specifications.md
│   │   ├── backlog.md
│   │   ├── sprints.md
│   │   └── maquettes/README.md
│   └── src/
│       ├── main/
│       └── test/
└── kanban-client/
    └── src/
```

---

## Auteurs

FANIRI Sanaa · LAMOUDNI Salma · DARIF Salma · BOUCETTA Salma  
Encadré par : Pr. SBAI Hanaa — FST Mohammedia — 2025/2026
