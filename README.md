# Course Management API

A backend application built with **Java 21 and Spring Boot** to manage climbing courses.

The project is designed as a practical **Senior Backend Java** portfolio project, demonstrating modern enterprise backend development, REST APIs, persistence, transaction management, concurrency control, relational and NoSQL databases, dynamic queries, aggregation, containerization, CI/CD, Kubernetes orchestration, Helm, microservices, distributed-system resilience, Apache Kafka, event-driven architecture, and modern deployment practices.

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
* Spring Kafka
* OpenAPI 3.0.3
* OpenAPI Generator 7.15.0
* Hibernate
* Jakarta Bean Validation
* SLF4J

## Databases

* PostgreSQL 17
* MongoDB 7

## Infrastructure

* Docker
* Docker Compose
* Apache Kafka 4.3 / KRaft
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
| Kafka / Event-Driven Architecture | [KAFKA.md](docs/KAFKA.md) |
| DDD / Hexagonal Architecture | [DDD.md](docs/DDD.md) |

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
Microservices                   ✅ 
        ↓
Kafka / Event-Driven            ✅ 
        ↓
Kafka broker / KRaft            ✅
        ↓
Topics / partitions / offsets   ✅
        ↓
Spring producer / consumer      ✅
        ↓
Consumer groups                 ✅
        ↓
Partition assignment            ✅
        ↓
Rebalancing / failover          ✅
        ↓
Consumer parallelism            ✅
        ↓
Event contracts / versioning    ✅
        ↓
Retry / DLT                     ✅
        ↓
Idempotency                     ✅
        ↓
DDD fundamentals                ✅
        ↓
Entities / Value Objects        ✅
        ↓
Aggregate Root / invariants     ✅
        ↓
Domain unit tests               ✅
        ↓
Inbound / outbound ports        ✅
        ↓
Application service             ✅
        ↓
Mongo persistence adapter       ✅
        ↓
Course REST adapter             ✅
        ↓
Spring composition root         ✅
        ↓
POST inbound adapter            ✅
        ↓
Create use case E2E             ✅
        ↓
Read-side port / service        ✅
        ↓
Mongo → Domain rehydration      ✅
        ↓
GET inbound adapter             ✅
        ↓
Read-side E2E validation        ✅
        ↓
Bounded Contexts / Context Map  ✅
        ↓
DDD / Hexagonal                 ✅
        ↓
Clean Architecture              ✅
        ↓
API-first design                ✅
        ↓
OpenAPI build validation        ✅
        ↓
Generated API models            ✅
        ↓
Generated API interface         ✅
        ↓
Security                        🚧 CURRENT
        ↓
Testing                         ⏳
        ↓
Advanced Backend Engineering    ⏳
        ↓
System Design                   ⏳
```

For detailed progress, **check** [ROADMAP.md](docs/ROADMAP.md).

---

# 🏗️ Technical Picture

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
* Deployment lifecycle

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

Because remote calls can fail in ways that local Java method calls cannot, the project implements **resilience** mechanisms around this communication.

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

# 📨 Event-Driven Communication

The project now also demonstrates asynchronous communication with **Apache Kafka** alongside the existing synchronous REST integration.

The Course application publishes a `CourseCreatedEvent` after a Course is created:

```text
POST /jpa/courses
        ↓
Course Service
        ↓
PostgreSQL
        ↓
CourseCreatedEvent
        ↓
KafkaTemplate
        ↓
course-events
```

The Enrollment Service consumes that event independently. The local learning topic now has two partitions, allowing two consumers in the same group to work in parallel:

```text
                     course-events
                    /             \
             Partition 0       Partition 1
                  │                 │
                  └───────┬─────────┘
                          │
             Consumer Group: enrollment-service
                          │
                 ┌────────┴────────┐
                 │                 │
          Enrollment #1     Enrollment #2
```

This gives the project both communication styles:

```text
Synchronous REST:
Enrollment → Course
        ↓
Caller waits for an immediate answer

Asynchronous Kafka:
Course → Kafka → Enrollment
        ↓
Consumer can process the event later
```

Current Kafka learning milestones include:

* Kafka 4.3 running in KRaft mode
* Reproducible `course-events` topic initialization
* Topics, partitions and partition-local offsets
* Spring Kafka producer with `KafkaTemplate`
* JSON event serialization
* Spring Kafka consumer with `@KafkaListener`
* Consumer group `enrollment-service`
* Committed offsets and consumer lag
* Consumer outage and catch-up recovery
* Multiple consumers in the same group
* Partition assignment and group rebalancing
* Automatic consumer failover
* Two-partition parallel consumption
* Kafka key, partition and offset inspection through `ConsumerRecord`
* Producer and consumer event contracts owned independently by each service
* Explicit event type and contract version
* Additive schema evolution and compatibility testing
* Breaking schema-change experiment
* `ErrorHandlingDeserializer` for deserialization failures
* Bounded processing retries with fixed backoff
* `course-events-dlt` Dead Letter Topic
* Poison-pill recovery without blocking partition progress
* Retry diagnostics through `RetryListener`
* EventId-based idempotent consumer
* MongoDB `processed_kafka_events` deduplication store
* Duplicate-event detection and skipping

---


# 🏛️ DDD / Hexagonal / Clean / API-First Architecture

The Enrollment Service has now been refactored through the core **DDD, Hexagonal Architecture, Ports and Adapters, Bounded Context, Clean Architecture, and API-first** milestones while preserving the working application.

Current direction:

```text
External world
      ↓
Inbound Adapter
      ↓
Inbound Port / Use Case
      ↓
Application Service
      ├───────────────┐
      ↓               ↓
Domain Model     Outbound Ports
                      ↓
            Technical Adapters
```

The framework-free Enrollment domain currently contains:

```text
Enrollment                       Entity + Aggregate Root
├── EnrollmentId                 Value Object
├── CourseId                     Value Object
├── StudentName                  Value Object
├── EnrollmentStatus             Domain concept
├── confirm()                    Domain behavior
└── cancel()                     Domain behavior
```

The application layer currently contains:

```text
application
├── port
│   ├── in
│   │   ├── CreateEnrollmentCommand
│   │   ├── CreateEnrollmentUseCase
│   │   └── FindEnrollmentsUseCase
│   └── out
│       ├── SaveEnrollmentPort
│       ├── FindEnrollmentsPort
│       └── CourseExistsPort
└── service
    ├── CreateEnrollmentService
    └── FindEnrollmentsService
```

`CreateEnrollmentService` implements the write-side inbound use case and orchestrates Course validation, domain construction and persistence through outbound ports.

`FindEnrollmentsService` implements the read-side use case and obtains domain aggregates through `FindEnrollmentsPort`.

Pure unit tests validate the domain model, both application services, and both outbound adapter directions without requiring a real MongoDB instance for unit-level mapping tests.

The **Create Enrollment** use case is now wired end-to-end through the Hexagonal architecture:

```text
POST /enrollments
      ↓
EnrollmentController
      ↓
CreateEnrollmentUseCase
      ↓
CreateEnrollmentService
      ├── CourseExistsPort
      │       ↑
      │   CourseRestAdapter
      │       ↓
      │   Course Service
      │
      └── SaveEnrollmentPort
              ↑
        MongoEnrollmentAdapter
              ↓
        EnrollmentRepository
              ↓
            MongoDB
```

The REST adapter now maps the created domain object to `EnrollmentResponse`, including the current `EnrollmentStatus`.

The read side is now also migrated end-to-end:

```text
GET /enrollments
      ↓
EnrollmentController
      ↓
FindEnrollmentsUseCase
      ↓
FindEnrollmentsService
      ↓
FindEnrollmentsPort
      ↑
MongoEnrollmentAdapter
      ↓
EnrollmentRepository
      ↓
MongoDB
      ↓
Enrollment.rehydrate(...)
      ↓
EnrollmentResponse
```

A real end-to-end test confirmed that an Enrollment persisted with `status = "CONFIRMED"` is returned as `CONFIRMED`, proving that rehydration restores persisted state instead of applying the new-aggregate default of `PENDING`.

The two business contexts are now formally separated:

```text
Course Context
    upstream
       │
       │ REST / Kafka published contracts
       ▼
Enrollment Context
    downstream
```

Enrollment does not import Course persistence or domain implementation classes. `CourseExistsPort` and `CourseRestAdapter` protect the Enrollment model from the upstream implementation.

The Clean Architecture dependency rule was also verified directly in the source tree:

```text
domain      → infrastructure    NO
application → infrastructure    NO
adapters    → application       YES
adapters    → domain            YES
```

Spring wiring is intentionally kept in `EnrollmentApplicationConfiguration`, which acts as the composition root.

The REST boundary is now also contract-first:

```text
openapi/enrollment-api.yaml
        ↓
OpenAPI Generator
        ↓
generated EnrollmentsApi
generated EnrollmentRequest
generated EnrollmentResponse
generated ErrorResponse
        ↓
EnrollmentController
        ↓
application use cases
        ↓
domain
```

The OpenAPI contract defines both operations and their important response behavior:

```text
GET  /enrollments
└── 200 → EnrollmentResponse[]

POST /enrollments
├── 201 → EnrollmentResponse
├── 400 → ErrorResponse
├── 404 → ErrorResponse
└── 503 → ErrorResponse
```

Maven validates the specification during the build. A deliberately broken OpenAPI version was tested and correctly failed the build. The same contract generates the HTTP request/response/error models and the `EnrollmentsApi` interface.

The generated request model carries Bean Validation constraints derived from the contract, such as `@Min(1)` for `courseId`. `EnrollmentController` implements the generated interface, so the HTTP mappings are now generated from the OpenAPI source of truth rather than duplicated manually in the controller.

The generated contract remains at the REST boundary:

```text
generated API classes → adapter/in/rest
application           → no OpenAPI dependency
domain                → no OpenAPI dependency
```

Detailed notes: [DDD.md](docs/DDD.md)

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
├── MongoDB
└── Kafka
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
├── Enrollment  → localhost:8081
└── Kafka       → localhost:8082

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
GHCR                     GHCR
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
* Apache Kafka / Spring Kafka
* Consumer groups, partition assignment, rebalancing, offsets, lag and parallelism
* Event contracts, schema evolution and versioning
* Retry strategies, poison-pill handling and Dead Letter Topics
* Idempotent consumers and duplicate-event handling
* Domain-driven design
* Entities, Value Objects, Aggregates and domain invariants
* Hexagonal architecture
* Inbound / outbound ports and adapters
* Bounded Contexts and Context Mapping
* Clean Architecture and dependency direction
* API-first design
* OpenAPI contract validation and code generation
* Generated API interfaces and boundary models
* Security
* Testing
* Observability
* Scalability
* System design

---

# 👨‍💻 Author

**Rubén Marín**

Backend Java Developer

Technologies, architecture patterns and practices explored in this project include:


`Java 21` · `Spring Boot 4.1` · `Spring Web` · `RestClient` · `Spring Boot Actuator` · `Jakarta Bean Validation` · `Spring Data JPA` · `Hibernate` · `PostgreSQL` · `Spring Data MongoDB` · `MongoDB` · `MongoTemplate` · `Spring Cloud Circuit Breaker` · `Spring Kafka` · `Apache Kafka` · `KRaft` · `Docker` · `Docker Compose` · `Trivy` · `GitHub Actions` · `GitHub Container Registry (GHCR)` · `Kubernetes` · `Helm` · `Microservices` · `Event-Driven Architecture` · `DDD` · `Bounded Contexts` · `Context Mapping` · `Hexagonal Architecture` · `Ports and Adapters` · `Clean Architecture` · `API-first` · `OpenAPI 3.0.3` · `OpenAPI Generator 7.15.0`