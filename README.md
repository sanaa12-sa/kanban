# Kanban App

Application Kanban collaborative en temps réel avec serveur Java TCP et client JavaFX.

## Architecture

Client JavaFX → TCP → Serveur Java → Hibernate → MySQL

## Prérequis

- Java 17
- Maven 3.9+
- MySQL 8.0
- Docker & Docker Compose

## Installation locale

### 1. Cloner le projet
git clone https://github.com/votre-repo/kanban.git
cd kanban

### 2. Configurer MySQL
CREATE DATABASE kanban_db;
Modifier kanban-server/src/main/resources/hibernate.cfg.xml

### 3. Lancer le serveur
cd kanban-server
mvn clean compile exec:java

### 4. Lancer le client
cd kanban-client
mvn clean javafx:run

## Docker

cd kanban-server
docker compose up --build
docker compose up -d
docker compose logs -f kanban-server
docker compose down
docker compose down -v

## Docker Swarm (Orchestration)

docker swarm init
docker stack deploy -c docker-compose.swarm.yml kanban
docker stack ps kanban
docker stack rm kanban

## Design Patterns

- Builder : User, Board, Task
- Composite : BoardComposite, ColumnComposite, TaskLeaf
- Adapter : EmailNotificationAdapter

## CI/CD Pipeline

GitHub Actions sur chaque push main/develop :
1. build-and-test : compile + 44 tests JUnit
2. docker-build : construit et teste image Docker
3. deploy-staging : deploie automatiquement sur staging (main uniquement)

## Monitoring

### Prometheus
- URL : http://localhost:9090
- Config : kanban-server/prometheus.yml

### Grafana
- URL : http://localhost:3000
- Login : admin / admin
- Dashboard : Kanban App Dashboard

### Logging (Logback)
- Console + fichier : logs/kanban.log
- Rotation quotidienne, 30 jours de retention
- Config : kanban-server/src/main/resources/logback.xml

### ELK Stack (optionnel)
Pour un monitoring avance, l'integration ELK est possible :
- Elasticsearch : stockage des logs
- Logstash : pipeline de traitement
- Kibana : visualisation
Logback est configure pour etre compatible avec Logstash via logback.xml

## Tests

cd kanban-server
mvn test
mvn test -Dtest=UserServiceTest
mvn test -Dtest=BoardServiceTest
mvn test -Dtest=TaskServiceTest
mvn test -Dtest=CompositePatternTest

## Structure du projet

kanban/
├── .github/workflows/ci.yml
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
