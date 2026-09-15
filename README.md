# Climbing Management API

A backend application built with **Java 21 and Spring Boot** to manage climbing courses.

The project is designed as a practical **Senior Backend Java** learning and portfolio project, demonstrating modern enterprise backend development, REST APIs, persistence, transaction management, concurrency control, relational and NoSQL databases, dynamic queries, aggregation, containerization, CI/CD, Kubernetes orchestration, Helm, microservices, distributed-system resilience, and modern deployment practices.

The application is developed incrementally, introducing technologies and architectural patterns commonly used in enterprise Java applications.

The project has now evolved from a single Spring Boot backend into a small **microservices architecture** with independent applications, persistence, container images, CI builds, GHCR packages, and Kubernetes deployments.

---

# 🚀 Tech Stack

## Backend

* Java 21
* Spring Boot 4.1
* Spring Web
* Spring `RestClient`
* Spring Data JPA
* Spring Data MongoDB
* Spring Boot Actuator
* Spring Cloud Circuit Breaker
* Hibernate
* Jakarta Bean Validation
* SLF4J

## Databases

* PostgreSQL 17
* MongoDB 7

## Infrastructure

* Docker
* Docker Compose
* Kubernetes
* Helm

## CI/CD

* GitHub Actions
* GitHub Container Registry (GHCR)
* Immutable commit-SHA image tags
* Continuous Delivery workflow

## Development Tools

* Maven 3.9+
* Maven Wrapper
* Git / GitHub
* IntelliJ IDEA
* Postman
* Docker Desktop
* kubectl
* Helm
* Trivy

---

# 🏗️ Current Technical Picture

The project currently contains two independent Spring Boot applications inside the same Git repository.

This is a **monorepo**:

```text
climbing-management-sb/
│
├── pom.xml
├── src/
│   └── Climbing Management API
│
└── services/
    └── enrollment-service/
        ├── pom.xml
        ├── src/
        └── Dockerfile
```

The two applications are independently runnable and independently deployable.

```text
                         Client
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
   Climbing Management API      Enrollment Service
         Spring Boot                Spring Boot
            :8080                     :8081
              │                         │
              │                         │
              │      REST / HTTP        │
              ◄─────────────────────────┘
                 Course validation
              │                         │
              ▼                         ▼
      PostgreSQL / MongoDB          MongoDB
                              enrollment_management
```

The Enrollment Service validates Course existence through the Course API before persisting an enrollment.

```text
POST /enrollments
        │
        ▼
Enrollment Service
        │
        ▼
Circuit Breaker
        │
        ▼
CourseClient
        │
        ▼
Retry
        │
        ▼
RestClient
        │
        ▼
Course API
        │
        ├── Course exists
        │       ↓
        │   save enrollment
        │       ↓
        │   201 Created
        │
        ├── Course missing
        │       ↓
        │   404 Not Found
        │
        └── Course unavailable / timeout
                ↓
            503 Service Unavailable
```

The Enrollment Service now has its own:

* Maven build
* Spring Boot runtime
* HTTP API
* JVM process
* MongoDB persistence
* database ownership
* Dockerfile
* Docker image
* GHCR package
* Kubernetes Deployment
* Kubernetes Service
* Kubernetes ConfigMap
* deployment lifecycle

Current Enrollment API:

```text
POST /enrollments
GET  /enrollments
```

---

# 🔄 Microservice Communication

The Enrollment Service communicates synchronously with the Course application using HTTP.

```text
Enrollment Service
        │
        ▼
CourseClient
        │
        ▼
Spring RestClient
        │
        ▼
Course API
```

Because remote calls can fail in ways that local Java method calls cannot, the project implements resilience mechanisms around this communication.

```text
Remote call
   │
   ├── Timeout
   │
   ├── Retry
   │
   └── Circuit Breaker
```

The current resilience flow is:

```text
Enrollment request
        ↓
Circuit Breaker
        ↓
CourseClient
        ↓
@Retryable
        ↓
RestClient
        ↓
Course Service
```

Implemented behavior includes:

* Connect timeout
* Read timeout
* Selective retry
* Bounded retry attempts
* `404` without retry
* `503` for unavailable dependencies
* Circuit Breaker
* CLOSED / OPEN / HALF-OPEN states
* Fail-fast behavior
* Automatic recovery
* Failure isolation
* Prevention of persistence after failed Course validation

---

# 🐳 Independent Containerization

Both applications have independent Docker build boundaries.

```text
Course application
        ↓
Dockerfile
        ↓
Course image
```

and:

```text
Enrollment Service
        ↓
Dockerfile
        ↓
Enrollment image
```

Docker Compose runs the complete environment:

```text
Docker Compose
│
├── Course application
├── Enrollment Service
├── PostgreSQL
└── MongoDB
```

Docker service discovery allows containers to communicate using service names instead of `localhost`.

Example:

```text
Enrollment container
        ↓
http://app:8080
        ↓
Course container
```

---

# ☸️ Kubernetes Deployment

Both Spring Boot applications can also run as independent Kubernetes workloads.

```text
Kubernetes
│
├── Course Deployment
│   └── Course Pods
│
├── climbing-management-service
│
├── Enrollment Deployment
│   └── Enrollment Pod
│
├── enrollment-service
│
├── PostgreSQL
└── MongoDB
```

Inside Kubernetes, the Enrollment Service communicates with the Course application through Kubernetes Service discovery:

```text
Enrollment Pod
        ↓
climbing-management-service:9090
        ↓
Course Pods
```

The Enrollment API is exposed internally through:

```text
enrollment-service:9091
        ↓
Enrollment Pod:8081
```

For local learning and testing, Docker and Kubernetes use distinct host ports:

```text
Docker Compose
├── Course      → localhost:8080
└── Enrollment  → localhost:8081

Kubernetes port-forward
├── Course      → localhost:9090
└── Enrollment  → localhost:9091
```

This makes it possible to test both environments independently.

---

# 📦 Independent CI/CD Artifacts

GitHub Actions builds and tests both Spring Boot applications.

```text
Git push
    ↓
GitHub Actions
    ↓
┌──────────────────────────┐
│                          │
▼                          ▼
Course build          Enrollment build
│                          │
▼                          ▼
Course JAR            Enrollment JAR
│                          │
▼                          ▼
Course image          Enrollment image
│                          │
▼                          ▼
GHCR                   GHCR
```

The Course image is published as:

```text
ghcr.io/rmarintech/climbing-management-sb
```

The Enrollment image is published as:

```text
ghcr.io/rmarintech/climbing-management-sb-enrollment-service
```

The CI pipeline publishes both:

```text
:latest
```

and:

```text
:<commit-sha>
```

tags.

The manual learning deployments currently use `latest`, while immutable commit-SHA images remain the preferred production-oriented deployment strategy.

---

# 📚 Detailed Documentation

Detailed learning material is split by technology so examples are not duplicated between files.

| Topic | Documentation |
| --- | --- |
| Course/project progress | [ROADMAP.md](docs/ROADMAP.md) |
| How to maintain these docs | [MAINTENANCE.md](docs/MAINTENANCE.md) |
| Spring Boot, REST, validation, exceptions | [SPRING_BOOT_REST.md](docs/SPRING_BOOT_REST.md) |
| PostgreSQL, JPA, transactions, locking | [POSTGRESQL_JPA.md](docs/POSTGRESQL_JPA.md) |
| MongoDB | [MONGODB.md](docs/MONGODB.md) |
| Docker | [DOCKER.md](docs/DOCKER.md) |
| CI/CD | [CICD.md](docs/CICD.md) |
| Kubernetes | [KUBERNETES.md](docs/KUBERNETES.md) |
| Helm | [HELM.md](docs/HELM.md) |
| Microservices | [MICROSERVICES.md](docs/MICROSERVICES.md) |

---

# 🗺️ Current Position

```text
Java / Spring Boot              ✅
        ↓
PostgreSQL / JPA                ✅
        ↓
Transactions / Locking          ✅
        ↓
MongoDB                         ✅
        ↓
Docker                          ✅
        ↓
CI/CD                           ✅
        ↓
Kubernetes                      ✅
        ↓
Namespaces                      ✅
        ↓
Helm                            ✅
        ↓
Microservice extraction         ✅
        ↓
Independent persistence         ✅
        ↓
REST communication              ✅
        ↓
Failure isolation               ✅
        ↓
Timeouts                        ✅
        ↓
Retries                         ✅
        ↓
Circuit Breaker                 ✅
        ↓
Resilience                      ✅
        ↓
Independent containerization    ✅
        ↓
Independent Kubernetes deploy   ✅
        ↓
Independent GHCR images         ✅
        ↓
Independent deployment          ✅
        ↓
Microservices                   ✅ COMPLETE
        ↓
Kafka / Event-Driven            🚧 CURRENT
        ↓
DDD / Hexagonal                 ⏳
        ↓
Security                        ⏳
        ↓
Testing                         ⏳
        ↓
Advanced Backend Engineering    ⏳
        ↓
System Design                   ⏳
```

For detailed progress, use **only** [ROADMAP.md](docs/ROADMAP.md).

---

# 🎯 Learning Approach

Each major topic is learned using the same practical loop:

```text
Theory
   ↓
Implementation
   ↓
Inspection
   ↓
API / Behaviour Testing
   ↓
Troubleshooting
   ↓
Interview-Level Understanding
   ↓
Code Cleanup
   ↓
Git Commit
   ↓
CI/CD Validation
```

The detailed examples for each step live in the corresponding topic document rather than in this README.

---

# 🎯 Project Goal

The goal is not only to create a working API.

The project is intended to demonstrate and reinforce the skills expected from a **Senior Backend Java Developer**, including:

* Java and Spring Boot
* REST API design
* Layered architecture
* Persistence
* Transaction management
* Concurrency control
* Relational databases
* NoSQL databases
* Containerization
* CI/CD
* Kubernetes
* Helm
* Distributed systems
* Microservices
* Service-to-service communication
* Resilience patterns
* Event-driven architecture
* Domain-driven design
* Hexagonal architecture
* Security
* Testing
* Observability
* Scalability
* System design

---

# 👨‍💻 Author

**Rubén Marín**

Backend Java Developer

Technologies explored in this project include:

`Java` · `Spring Boot` · `Spring Web` · `RestClient` · `Spring Data JPA` · `Hibernate` · `PostgreSQL` · `Spring Data MongoDB` · `MongoDB` · `MongoTemplate` · `Spring Cloud Circuit Breaker` · `Docker` · `Docker Compose` · `GitHub Actions` · `GitHub Container Registry` · `Kubernetes` · `Helm` · `Microservices`
