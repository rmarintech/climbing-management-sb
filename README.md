# Climbing Management API

A backend application built with **Java 21 and Spring Boot** to manage climbing courses.

## 📚 Documentation

This documentation is split into three Markdown files so the repository stays readable while preserving the detailed course material:

- **[README.md](README.md)** — project overview, architecture, current status, roadmap, goals, running instructions, examples, and learning approach.
- **[README-BACKEND.md](README-BACKEND.md)** — REST API, PostgreSQL/JPA, Hibernate, transactions, locking, validation, MongoDB, queries, indexes, aggregation, `$lookup`, and MongoDB transactions.
- **[README-INFRASTRUCTURE.md](README-INFRASTRUCTURE.md)** — Docker, Docker Compose, security, healthchecks, Trivy, GitHub Actions, GHCR, Continuous Delivery, Kubernetes, Ingress, HPA, namespaces, and Helm.

## 🚦 Current Project Status

### Completed infrastructure milestones

- [x] Kubernetes namespaces
- [x] Helm Charts
- [x] Helm templates and values
- [x] Helm releases
- [x] Immutable Docker image deployment using Git commit SHA
- [x] Helm deployment of immutable commit-SHA images

Latest milestones:

- `feat(k8s): introduce namespaces`
- `feat(helm): deploy immutable commit SHA images`

**Kubernetes + Helm infrastructure is now complete.**

### Current course phase

**Senior Backend Java application architecture and engineering**

### Next topic

**Advanced REST API design**

See the full roadmap below for the remaining course topics.

---



# 🚀 Tech Stack

## Backend

* **Java 21**
* **Spring Boot 4.1**
* **Spring Web**
* **Spring Data JPA**
* **Spring Data MongoDB**
* **Spring Boot Actuator**
* **Hibernate**
* **Jakarta Bean Validation**
* **SLF4J / Logging**

## Databases

* **PostgreSQL 17**
* **MongoDB 7**

## Development Tools

* **Maven 3.9+**
* **Git / GitHub**
* **IntelliJ IDEA**
* **Postman**

## Infrastructure

* **Docker**
* **Docker Compose**
* **Docker Secrets**
* **Docker Healthchecks**
* **Docker multi-stage builds**
* **Trivy vulnerability scanning**
* **Kubernetes**
* **Helm**

## CI/CD

* **GitHub Actions**
* **Maven CI builds**
* **Automated tests**
* **GitHub Actions service containers**
* **JAR artifact publishing**
* **GitHub Container Registry (GHCR)**
* **Docker image build and publishing**
* **Immutable Docker image tags**
* **Continuous Delivery**
* **Production deployment approval**
* **Helm-based Kubernetes deployment**
* **Immutable commit-SHA deployments**

## Kubernetes

* **Kubernetes Deployments**
* **Pods**
* **ReplicaSets**
* **Services**
* **ClusterIP**
* **Kubernetes DNS / service discovery**
* **Namespaces**
* **ConfigMaps**
* **Secrets**
* **Startup probes**
* **Readiness probes**
* **Liveness probes**
* **Rolling updates**
* **Multiple application replicas**
* **EndpointSlices**
* **PersistentVolumes**
* **PersistentVolumeClaims**
* **StorageClasses**
* **Persistent PostgreSQL storage**
* **Resource requests and limits**
* **Metrics Server**
* **Horizontal Pod Autoscaler (HPA)**
* **Ingress**
* **NGINX Ingress Controller**
* **kubectl port-forward**

## Helm

* **Helm Charts**
* **Helm templates**
* **Helm values**
* **Helm releases**
* **Kubernetes deployment through Helm**
* **Immutable Docker image references**
* **Commit-SHA based image deployment**

---

# 🏗️ Architecture

The application follows a layered architecture:

```text
REST API
   │
   ▼
Controller
   │
   ▼
Service
   │
   ├──────────────────────┐
   ▼                      ▼
Repository          Custom Repository
   │                      │
   ▼                      ▼
Spring Data          MongoTemplate
   │                      │
   ▼                      ▼
PostgreSQL             MongoDB
```
The application is also deployed as a Kubernetes workload:

## Kubernetes

```text
Namespace
   │
   ▼
Ingress
   │
   ▼
Application Service
   │
   ├───────────────┐
   ▼               ▼
App Pod          App Pod
   │               │
   └───────┬───────┘
           │
           ├──────────────────┐
           ▼                  ▼
 PostgreSQL Service      MongoDB Service
           │                  │
           ▼                  ▼
 PostgreSQL Pod          MongoDB Pod
           │
           ▼
        PVC / PV
```

Kubernetes resources can be packaged and deployed using Helm:

```text
Helm Chart
   │
   ├───────────────┐
   ▼               ▼
Chart templates   values.yaml
   │               │
   └───────┬───────┘
           ▼
Kubernetes manifests
           │
           ▼
Helm release
           │
           ▼
Kubernetes
           │
           ▼
Namespace
   │
   ├──────────┬──────────┐
   ▼          ▼          ▼
App Pods   PostgreSQL   MongoDB
```

The CI/CD deployment flow is:

```text
Git push
   │
   ▼
GitHub Actions
   │
   ├── Build
   ├── Test
   ├── Package JAR
   └── Build Docker image
          │
          ▼
        GHCR
          │
          ▼
   commit-SHA image
          │
          ▼
         Helm
          │
          ▼
Kubernetes release
          │
          ▼
Application
```

## Main layers

Controller

Exposes the REST API and handles HTTP requests and responses.

Service

Contains business logic and transaction boundaries.

Repository

Provides data access through Spring Data repositories.

Custom Repository

Used when standard repository methods are not sufficient, for example dynamic MongoDB queries and aggregation pipelines.

Entity / Document

Represents the persistence model for PostgreSQL and MongoDB.

DTO / Record

Defines the API contract independently from the persistence model.


---

## 📖 Detailed documentation

For the complete feature and infrastructure documentation, use:

- [Backend / Application Documentation](README-BACKEND.md)
- [Infrastructure / DevOps Documentation](README-INFRASTRUCTURE.md)

# 🗺️ Roadmap

The project is being developed progressively.

### Completed
- [x] Spring Boot project setup
- [x] Java 21 configuration
- [x] REST API
- [x] Layered architecture
- [x] DTOs / Java Records
- [x] Bean Validation
- [x] Global Exception Handling
- [x] SLF4J logging
- [x] PostgreSQL
- [x] Spring Data JPA
- [x] Hibernate
## Derived Queries
## JPQL
- [x] @Transactional
- [x] Transaction propagation
- [x] REQUIRED
- [x] REQUIRES_NEW
- [x] Rollback rules
- [x] Hibernate Dirty Checking
## Optimistic Locking
## Pessimistic Locking
- [x] MongoDB
- [x] Spring Data MongoDB
- [x] MongoRepository
- [x] MongoDB operators
- [x] MongoDB pagination
- [x] MongoDB sorting
- [x] MongoDB DTOs
- [x] MongoDB validation
- [x] MongoDB indexes
- [x] Compound indexes
- [x] MongoDB explain()
- [x] MongoTemplate
- [x] Dynamic MongoDB queries
### MongoDB aggregation
- [x] Aggregation DTO mapping
### MongoDB $lookup
- [x] Embedded vs referenced document modelling
- [x] MongoDB replica set configuration
- [x] MongoDB transactions
- [x] MongoDB transaction rollback
- [x] Multi-collection MongoDB transactions
- [x] MongoDB interview comparison with PostgreSQL
- [x] Docker fundamentals
- [x] Docker images and containers
## Dockerfile
- [x] Docker image layers and build cache
- [x] Docker port mapping
- [x] Docker environment variables
- [x] Docker volumes
- [x] Docker networks
- [x] PostgreSQL container
- [x] MongoDB container
### Docker Compose
- [x] Docker Compose service discovery
- [x] Multi-stage Docker builds
- [x] Alpine-based runtime image
- [x] Non-root container user
- [x] Read-only container filesystem
- [x] Docker tmpfs
- [x] Docker Secrets
- [x] Spring Boot Docker profile
- [x] Spring Boot Actuator
- [x] Docker healthchecks
- [x] Liveness / readiness concepts
- [x] Docker troubleshooting
- [x] Container inspection with docker inspect
- [x] Docker DNS troubleshooting
- [x] Trivy vulnerability scanning
- [x] Dependency vulnerability remediation
- [x] GitHub Actions CI
- [x] CI service containers
- [x] Automated Maven build and tests
- [x] JAR artifact publishing
- [x] Docker image build in CI
- [x] GitHub Container Registry
- [x] Docker image publishing to GHCR
- [x] Docker image tagging strategy
- [x] Immutable commit SHA image tags
- [x] Continuous Delivery
- [x] GitHub Actions production environment
- [x] Deployment approval gate
- [x] Kubernetes cluster
- [x] Kubernetes Pods
- [x] Kubernetes Deployments
- [x] Kubernetes ReplicaSets
- [x] Kubernetes Services
- [x] Kubernetes ClusterIP
- [x] Kubernetes service discovery
- [x] Kubernetes DNS
- [x] Kubernetes namespaces
- [x] Kubernetes ConfigMaps
- [x] Kubernetes Secrets
- [x] Kubernetes startup probes
- [x] Kubernetes readiness probes
- [x] Kubernetes liveness probes
- [x] Kubernetes rolling updates
- [x] Kubernetes multiple application replicas
- [x] Kubernetes EndpointSlices
- [x] Kubernetes database Services
- [x] Kubernetes local application testing
- [x] Kubernetes PersistentVolumes
- [x] Kubernetes PersistentVolumeClaims
- [x] Kubernetes StorageClasses
- [x] PostgreSQL persistent storage
- [x] Kubernetes resource requests
- [x] Kubernetes resource limits
- [x] Kubernetes Metrics Server
- [x] Kubernetes kubectl top
- [x] Kubernetes Horizontal Pod Autoscaler
- [x] HPA CPU-based scaling
- [x] HPA load testing
- [x] Kubernetes Ingress
- [x] NGINX Ingress Controller
- [x] Ingress routing
- [x] Local Ingress testing
## Helm
- [x] Helm Charts
- [x] Helm templates
- [x] Helm values
- [x] Helm releases
- [x] Helm-based Kubernetes deployment
- [x] Immutable commit-SHA image deployment through Helm
### Current

The Kubernetes and Helm infrastructure phase is complete.

The next phase is Senior Backend Java application architecture and engineering.

### Upcoming
- [ ] Advanced REST API design
- [ ] Spring Security
- [ ] Unit testing
- [ ] Integration testing
- [ ] Testcontainers
- [ ] Event-driven architecture
- [ ] Messaging / Kafka
- [ ] Microservices
- [ ] DDD
- [ ] Hexagonal Architecture
- [ ] System design
- [ ] Performance and scalability
- [ ] Senior Backend Java interview preparation
# 🎯 Project Goals

The main goal of this project is to build a realistic backend application while demonstrating practical knowledge of enterprise Java technologies.

Key areas include:

- REST API design
- [x] Layered architecture
- Separation of responsibilities
- DTO-based API contracts
- Persistence and ORM
- Transaction management
- [x] Transaction propagation
- Rollback behavior
- Hibernate dirty checking
- Optimistic locking
- Pessimistic locking
- Exception handling
- [x] Bean Validation
- Relational databases
- NoSQL databases
- MongoDB data modelling
- MongoDB indexing
- Query performance analysis
- Dynamic queries
- Aggregation pipelines
- Collection joins using $lookup
- [x] MongoDB transactions
- Containerization
### Docker Compose
- Docker security
- [x] Docker Secrets
- Healthchecks
- Vulnerability scanning
## CI/CD
- GitHub Actions
- Container registries
- Immutable image versioning
## Kubernetes
- [x] Kubernetes namespaces
- Container orchestration
- Service discovery
- Persistent storage
- Resource management
- Horizontal autoscaling
- Ingress and HTTP routing
- Health probes
## Helm
- Kubernetes application packaging
- [x] Helm releases
- Immutable Helm deployments
- Distributed systems
- Messaging
- [x] Microservices
- Domain-driven design
- Clean architecture

The project is also used as a practical learning environment for Senior Backend Java development and technical interview preparation.

# 🛠️ Running the Project
### Requirements
### Local development
Java 21
Maven 3.9+
PostgreSQL 17
MongoDB 7+
Git
### Docker development
Docker Desktop
### Docker Compose
### Kubernetes development
Docker Desktop with Kubernetes enabled
`kubectl`
## Helm

The recommended development approach is to run infrastructure and application components through Docker Compose or Kubernetes depending on the learning scenario.

Clone the repository:

git clone https://github.com/rmarintech/climbing-management-sb.git
cd climbing-management-sb
### Maven

Run the test suite:

`mvn clean test`

Build the application:

`mvn clean package`

Run the application locally:

`mvn spring-boot:run "-Dspring-boot.run.profiles=local"`

The API will be available at:

`http://localhost:8080`
### Docker Compose

Start the complete environment:

`docker compose up -d --build`

Check the containers:

`docker compose ps`

View application logs:

`docker compose logs app`

Follow application logs:

`docker compose logs -f app`

Stop the environment:

`docker compose down`

The application will be available at:

`http://localhost:8080`

**Healthcheck:**

`http://localhost:8080/actuator/health`

**Expected response:**

`{`
"groups": [
"liveness",
"readiness"
],
"status": "UP"
`}`
## ☸️ Kubernetes

The application can be deployed using the Kubernetes manifests or through the Helm chart.

The Kubernetes deployment is organized into a dedicated namespace.

Create the namespace if it does not already exist:

`kubectl create namespace climbing-management`

If it already exists, Kubernetes returns an error indicating that the namespace is already present.

Check namespaces:

`kubectl get namespaces`

Check resources in the application namespace:

`kubectl get all -n climbing-management`
### Kubernetes Manifest Deployment

Apply the application configuration:

`kubectl apply -f k8s/app-config.yaml -n climbing-management`
`kubectl apply -f k8s/app-secret.yaml -n climbing-management`

Deploy PostgreSQL storage:

`kubectl apply -f k8s/postgres-pvc.yaml -n climbing-management`

Deploy PostgreSQL:

`kubectl apply -f k8s/postgres-deployment.yaml -n climbing-management`
`kubectl apply -f k8s/postgres-service.yaml -n climbing-management`

Deploy MongoDB:

`kubectl apply -f k8s/mongo-deployment.yaml -n climbing-management`
`kubectl apply -f k8s/mongo-service.yaml -n climbing-management`

Deploy the application:

`kubectl apply -f k8s/app-deployment.yaml -n climbing-management`
`kubectl apply -f k8s/app-service.yaml -n climbing-management`

**Check the cluster:**

`kubectl get pods -n climbing-management`

**Expected application state:**

climbing-management-xxxxx   1/1   Running
climbing-management-xxxxx   1/1   Running

**Check Services:**

`kubectl get services -n climbing-management`

**Check EndpointSlices:**

`kubectl get endpointslices -n climbing-management`

**Check persistent storage:**

`kubectl get pvc -n climbing-management`
`kubectl get pv`
`kubectl get storageclass`

**Check resource usage:**

`kubectl top pods -n climbing-management`
`kubectl top nodes`

**Check the application Deployment:**

`kubectl get deployment climbing-management -n climbing-management`

**Check the HPA:**

`kubectl get hpa -n climbing-management`

**Check rollout status:**

`kubectl rollout status deployment/climbing-management -n climbing-management`
⛵ Helm Deployment

The preferred packaged Kubernetes deployment uses Helm.

The basic Helm workflow is:

Helm Chart
`↓`
- [x] Helm values
  `↓`
  Helm render
  `↓`
  Helm release
  `↓`
  Kubernetes namespace

**Inspect the chart before deployment:**

`helm lint <chart-path>`

**Render the Kubernetes resources locally:**

`helm template <release-name> <chart-path> -n climbing-management`

**Install the application:**

`helm install <release-name> <chart-path> -n climbing-management`

**Upgrade the application:**

`helm upgrade <release-name> <chart-path> -n climbing-management`

**List Helm releases:**

`helm list -n climbing-management`

**Inspect the release:**

`helm status <release-name> -n climbing-management`

**Inspect release history:**

`helm history <release-name> -n climbing-management`

The exact chart path and release name are determined by the Helm chart structure in the repository.

🔒 Helm Immutable Image Deployment

The Docker image deployed through Helm should use the immutable commit SHA generated by CI.

**Conceptually:**

Git commit
`↓`
commit SHA
`↓`
GHCR image
`↓`
- [x] Helm values
  `↓`
  Kubernetes Deployment

**Example image:**

`ghcr.io/rmarintech/climbing-management-sb:<commit-sha>`

The deployment therefore identifies an exact image rather than using the mutable latest tag.

This is the deployment model used by the latest Helm milestone.

## Kubernetes Local API Access

**Forward the Kubernetes Service to the local machine:**

`kubectl port-forward -n climbing-management service/climbing-management-service 8080:8080`

Then access:

`http://localhost:8080`

**Health:**

`http://localhost:8080/actuator/health`

**Liveness:**

`http://localhost:8080/actuator/health/liveness`

**Readiness:**

`http://localhost:8080/actuator/health/readiness`
## Kubernetes Ingress Access

Forward the NGINX Ingress Controller locally:

`kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8081:80`

**Then test the Ingress routing:**

`curl.exe -H "Host: climbing-management.local" http://localhost:8081/actuator/health`

**Expected response:**

up
# 📮 Example Requests
### Create a PostgreSQL course
`POST /jpa/courses`
Content-Type: application/json
`{`
"name": "Sport Climbing",
"price": 120.0,
"difficulty": "EASY"
`}`

**Example response:**

`{`
"id": 1,
"name": "Sport Climbing",
"price": 120.0,
"difficulty": "EASY"
`}`
### Create a MongoDB course
`POST /mongo/courses`
Content-Type: application/json
`{`
"name": "Via Ferrata",
"price": 90.0,
"difficulty": "EASY"
`}`
### Dynamic MongoDB search
`GET /mongo/courses/dynamic-search?difficulty=MEDIUM&minPrice=100&maxPrice=160&page=0&size=2&sort=price,asc`
### MongoDB aggregation
`GET /mongo/courses/difficulty-stats`

Returns statistics grouped by course difficulty.

### MongoDB $lookup
`GET /mongo/courses/with-enrollments`

Returns courses together with their related enrollments.

# 📌 Learning Approach

The project is intentionally developed incrementally.

Each major technology is introduced through:

Theory
`↓`
Implementation
`↓`
Database inspection
`↓`
API testing
`↓`
Concurrency / behaviour testing
`↓`
Troubleshooting
`↓`
Interview questions
`↓`
Code cleanup
`↓`
Git commit
`↓`
CI/CD validation

Each major milestone is committed to Git so the repository provides a clear history of the technologies and concepts implemented.

The CI/CD pipeline additionally validates the project automatically after every push to main, builds the application, produces the JAR artifact, builds and publishes the Docker image to GHCR, and supports controlled Continuous Delivery.

The latest infrastructure milestones also demonstrate:

Docker
`↓`
- GitHub Actions
  `↓`
  GHCR
  `↓`
  Immutable commit-SHA image
  `↓`
## Helm
`↓`
Kubernetes namespace
`↓`
Kubernetes Deployment

This provides a realistic end-to-end application delivery path from source code to a containerized Kubernetes workload.

# 👨‍💻 Author

Rubén Marín

Backend Java Developer

Technologies explored in this project include:

Java · Spring Boot · Spring Data JPA · Hibernate · PostgreSQL · Spring Data MongoDB · MongoDB · MongoTemplate · Docker · Docker Compose · GitHub Actions · GitHub Container Registry · Kubernetes · Helm
