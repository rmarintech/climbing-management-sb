# Climbing Management Backend

A backend system built with Java 21 and Spring Boot for managing climbing courses and enrollments.

The project is designed as a practical Senior Backend Java portfolio project, demonstrating modern enterprise backend development, REST APIs, persistence, transaction management, concurrency control, relational and NoSQL databases, containerization, CI/CD, Kubernetes, Helm, microservices, distributed-system resilience, Apache Kafka, event-driven architecture, DDD, Hexagonal Architecture, API-first development, OAuth2/JWT security, observability with Micrometer, Prometheus, Grafana, OpenTelemetry, Tempo and Loki, and modern deployment practices.

The application has been developed incrementally, introducing technologies and architectural patterns commonly used in enterprise Java applications.

The project has evolved from a single Spring Boot backend into a small microservices architecture with independently runnable services, separate persistence boundaries, REST and event-driven communication, independent container images, CI builds, GHCR packages, Kubernetes deployments, and centralized authentication with Keycloak.

---

# 🚀 Tech Stack

## Backend

* Java 21
* Spring Boot 4.1
* Spring Web
* Spring Security (HTTP Basic, OAuth2 Resource Server, JWT and role-based authorization)
* Keycloak (OAuth2 / OpenID Connect)
* Spring `RestClient`
* Spring Data JPA
* Spring Data MongoDB
* Spring Boot Actuator
* Micrometer
* Micrometer Tracing / Observation
* OpenTelemetry
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
* Prometheus 3.14
* Grafana 13.2
* Grafana Tempo 3.0.3
* Grafana Loki 3.7
* Grafana Alloy

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
* JUnit 5
* Mockito
* Testcontainers

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
| Spring Security, HTTP Basic, RBAC, CSRF, stateless authentication, OAuth2/OIDC, JWT and Keycloak | [SECURITY.md](docs/SECURITY.md) |
| Testing, Mockito, integration tests and Testcontainers | [TESTING.md](docs/TESTING.md) |
| Observability, Prometheus, Grafana, OpenTelemetry, Tempo, structured logs, Alloy and Loki | [OBSERVABILITY.md](docs/OBSERVABILITY.md) |

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
HTTP Basic / RBAC / CSRF        ✅
        ↓
OAuth2 / OIDC / JWT             ✅
        ↓
Keycloak Resource Server        ✅
        ↓
JWT audience / role mapping     ✅
        ↓
OpenAPI security alignment      ✅
        ↓
Security                        ✅
        ↓
Unit testing                    ✅
        ↓
Mockito                         ✅
        ↓
Mongo Testcontainers            ✅
        ↓
REST MVC / security tests       ✅
        ↓
Full HTTP integration tests     ✅
        ↓
Testing                         ✅
        ↓
Observability fundamentals      ✅
        ↓
Micrometer / Actuator metrics   ✅
        ↓
Prometheus / PromQL             ✅
        ↓
HTTP histograms / percentiles   ✅
        ↓
Grafana HTTP dashboard          ✅
        ↓
JVM saturation / GC metrics     ✅
        ↓
Kafka messaging observability   ✅
        ↓
Custom Micrometer metrics       ✅
        ↓
Business metrics / success rate ✅
        ↓
Distributed tracing             ✅
        ↓
Trace-log correlation           ✅
        ↓
Structured logging              ✅
        ↓
Loki / Alloy centralized logs   ✅
        ↓
Logs ↔ Traces correlation       ✅
        ↓
SLIs / SLOs                     ✅
        ↓
Grafana alerting                ✅
        ↓
Observability                   ✅
        ↓
API versioning                  ⏳ NEXT
        ↓
Advanced Backend Engineering    🚧 CURRENT
        ↓
System Design                   ⏳
```

For detailed progress, **check** [ROADMAP.md](docs/ROADMAP.md).

The **Testing and Observability phases are complete** for the planned course scope. Metrics flow through Micrometer / Actuator → Prometheus → Grafana; traces through Micrometer Tracing / OpenTelemetry → Tempo → Grafana; structured JSON logs through Alloy → Loki → Grafana. HTTP and Kafka trace propagation, bidirectional Loki ↔ Tempo navigation, availability and latency SLIs, error budgets, burn-rate monitoring, multi-window SLO alerts and webhook delivery are verified. The next Advanced Backend Engineering milestone is **API versioning**.

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

The OpenAPI contract defines both operations and their important response behavior, including the security boundary:

```text
GET /enrollments
├── 200 → EnrollmentResponse[]
├── 401 → Unauthorized
└── 403 → Forbidden

POST /enrollments
├── 201 → EnrollmentResponse
├── 400 → ErrorResponse
├── 401 → Unauthorized
├── 403 → Forbidden
├── 404 → ErrorResponse
└── 503 → ErrorResponse
```

The contract now defines an HTTP Bearer scheme with `bearerFormat: JWT`, and both GET and POST declare `bearerAuth` as an operation security requirement. The concrete role rules remain runtime concerns in Spring Security: GET allows USER or ADMIN, while POST requires ADMIN.

Maven validates the specification during the build. A deliberately broken OpenAPI version was tested and correctly failed the build. The same contract generates the HTTP request/response/error models and the `EnrollmentsApi` interface. The OpenAPI YAML is therefore a real build input and must also be copied into the Enrollment Service Docker builder before Maven runs.

The generated request model carries Bean Validation constraints derived from the contract, such as `@Min(1)` for `courseId`. `EnrollmentController` implements the generated interface, so the HTTP mappings are now generated from the OpenAPI source of truth rather than duplicated manually in the controller.

The generated contract remains at the REST boundary:

```text
generated API classes → adapter/in/rest
application           → no OpenAPI dependency
domain                → no OpenAPI dependency
```

Detailed notes: [DDD.md](docs/DDD.md)

---


# 🔐 Security

The Enrollment Service is now protected as an **OAuth2 Resource Server** backed by Keycloak.

The learning path intentionally progressed through two stages:

```text
HTTP Basic + RBAC
        ↓
CSRF experiment
        ↓
Explicit STATELESS policy
        ↓
OAuth2 / OpenID Connect
        ↓
Keycloak
        ↓
Authorization Code + PKCE
        ↓
JWT Bearer authentication
        ↓
Issuer + audience validation
        ↓
Keycloak roles → Spring authorities
        ↓
RBAC
```

Local identity provider:

```text
Keycloak
http://localhost:8083
realm: climbing
```

Postman is registered as the OAuth2 client:

```text
climbing-postman
```

The Enrollment API is the protected resource audience:

```text
enrollment-service
```

The verified token contains the expected issuer and audience:

```text
iss
→ http://localhost:8083/realms/climbing

aud
→ enrollment-service
→ account
```

Realm roles are read from:

```text
realm_access.roles
```

and converted into Spring Security authorities:

```text
USER
    ↓
ROLE_USER

ADMIN
    ↓
ROLE_ADMIN
```

Current request flow:

```text
User
    ↓
Keycloak login
    ↓
Authorization Code + PKCE
    ↓
Postman receives access token
    ↓
Authorization: Bearer <JWT>
    ↓
Enrollment Service
    ↓
Spring Security Resource Server
    ↓
signature / issuer / expiration / audience
    ↓
role conversion
    ↓
RBAC
    ↓
EnrollmentController
```

Verified behavior includes the complete runtime matrix:

```text
GET  + no token     → 401
GET  + ruben/USER   → 200
GET  + admin/ADMIN  → 200

POST + no token     → 401
POST + ruben/USER   → 403
POST + admin/ADMIN  → 201
```

The OpenAPI security contract is now aligned with the runtime model:

```text
components.securitySchemes.bearerAuth
        ↓
HTTP Bearer / JWT
        ↓
GET + POST security requirement
        ↓
reusable 401 / 403 responses
```

OpenAPI documents the authentication boundary and possible security responses; Spring Security still performs the runtime JWT validation and RBAC enforcement.

The OpenAPI YAML is also a Maven build input. The Enrollment Service Docker builder now copies `openapi/` before running Maven. This fixed the CI Docker failure where `/app/openapi/enrollment-api.yaml` was missing, and the pipeline returned to green.

Detailed notes: [SECURITY.md](docs/SECURITY.md)

---

# 🧪 Testing

The planned Testing phase is complete and now covers multiple levels, from framework-free domain tests to full service-level HTTP integration.

```text
Pure domain tests
        ↓
Application tests with fake ports
        ↓
Application tests with Mockito
        ↓
Mongo persistence integration
        ↓
REST MVC / security slice
        ↓
Full HTTP integration
        ✅
```

Current automated coverage includes:

```text
Domain / application
├── pure JUnit tests
├── hand-written fake ports
├── Mockito @Mock / @InjectMocks
├── stubbing and interaction verification
└── ArgumentCaptor

Mongo persistence
├── @DataMongoTest
├── MongoDB Testcontainer
├── @ServiceConnection
├── Domain → Mongo persistence
└── Mongo → Domain rehydration

REST MVC / security
├── @WebMvcTest
├── MockMvc
├── @MockitoBean
├── real SecurityConfiguration
├── mock JWT authentication
├── GET: 401 / USER 200 / ADMIN 200
├── POST: 401 / USER 403 / ADMIN 201
├── request / response mapping
└── generated Bean Validation → 400

Full application HTTP integration
├── @SpringBootTest
├── @AutoConfigureMockMvc
├── real Enrollment controller
├── real application services
├── real MongoEnrollmentAdapter
├── real EnrollmentRepository
├── real MongoDB Testcontainer
├── mocked JwtDecoder / external identity boundary
├── mocked CourseRestAdapter / external Course Service boundary
├── GET persisted data → 200 JSON
├── POST existing Course → 201 + persisted document
└── POST missing Course → 404 + MongoDB unchanged
```

The full integration tests intentionally mock only boundaries outside the Enrollment Service. Internal Enrollment components remain real, so the request path is exercised through controller, application service, persistence adapter, repository and MongoDB.

Detailed theory and examples: [TESTING.md](docs/TESTING.md)

---

# 📈 Observability

The current **Advanced Backend Engineering** phase is focused on making the two services observable through correlated metrics, traces and logs.

The local metrics flow is:

```text
Enrollment Service
    │
    │ Micrometer instrumentation
    ▼
Spring Boot Actuator
    │
    │ /actuator/prometheus
    ▼
Prometheus
    │
    │ PromQL
    ▼
Grafana
    │
    ▼
Dashboards
```

The local tracing flow is:

```text
Course Service / Enrollment Service
    │
    │ Micrometer Tracing / Observation
    ▼
OpenTelemetry
    │
    │ OTLP HTTP :4318
    ▼
Tempo
    │
    ▼
Grafana Explore
```

The local logging flow is:

```text
Enrollment Service
    ↓ structured JSON
logs/enrollment-service.log
    ↓
Grafana Alloy
    ↓
Loki
    ↓
Grafana Explore
```

Current local endpoints:

```text
Course Service
→ http://localhost:8080

Enrollment Service
→ http://localhost:8081

Prometheus
→ http://localhost:9090

Tempo
→ http://localhost:3200

Loki
→ http://localhost:3100

Alloy
→ http://localhost:12345

Grafana
→ http://localhost:3000
```

Prometheus currently runs as a standalone Docker container and scrapes the Enrollment Service running on the Windows host through:

```text
host.docker.internal:8081/actuator/prometheus
```

The application uses the Prometheus Micrometer registry and exposes the required Actuator endpoints:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

The HTTP histogram configuration enables cumulative `_bucket` metrics so Prometheus can calculate latency percentiles with `histogram_quantile(...)`.

PromQL work completed so far includes:

```text
HTTP request counters
rate(...)
increase(...)
sum(...)
sum by (...)
5xx error rate
average HTTP latency
histogram buckets
p50 / p90 / p95 / p99 latency
```

The Grafana dashboard currently contains four main areas:

```text
Enrollment Service — HTTP Overview
├── Enrollment Traffic
├── Enrollment 5xx Error Rate
├── Enrollment GET p50 Latency
├── Enrollment GET p95 Latency
└── Enrollment GET p99 Latency

Enrollment Service — Runtime / Saturation
├── Enrollment JVM CPU
├── Enrollment JVM Heap Memory
├── Enrollment JVM Heap Utilization
└── Enrollment JVM GC Pause

Enrollment Service — Dependencies / Messaging
├── Kafka Lag by Partition
├── Kafka Total Consumer Lag
├── Kafka Assigned Partitions
├── Kafka Consumer Throughput
├── Kafka Listener Processing Time
├── Kafka Listener Failure Rate
├── Kafka DLT Events
└── Kafka Duplicate Events Skipped

Enrollment Service — Business Metrics
├── Enrollments Created
├── Course Validation Failures
└── Enrollment Creation Success Rate
```

The heap panel demonstrates the expected JVM sawtooth pattern:

```text
object allocation
    ↓
used heap rises
    ↓
garbage collection
    ↓
memory reclaimed
    ↓
used heap drops
```

The Kafka panels now show backlog, ownership, throughput, listener execution and application-specific failure handling. A deliberate processing failure and a malformed Kafka payload verified the tagged DLT counter with `reason="processing"` and `reason="deserialization"`. Replaying the same `eventId` verified the duplicate-event counter.

Business instrumentation is kept outside the framework-free application layer through `EnrollmentMetricsPort` and a Micrometer outbound adapter. The application records creation attempts, successful persisted Enrollments and missing-Course validation failures. Grafana derives a recent Enrollment creation success rate from the success and attempt counters. A longer local query window is used for the ratio because the tiny learning workload makes short sliding windows volatile.

The custom Micrometer / business-metrics milestone is complete.

Distributed tracing is also complete for the current course scope:

```text
HTTP
Enrollment Service
    ↓ traceparent
Course Service

Kafka
Course Service
    ↓ trace context in Kafka record headers
Enrollment Service
```

Both services export traces through OpenTelemetry to Tempo. Kafka producer and consumer observations preserve the same distributed `traceId` across the asynchronous boundary, while each span has its own `spanId`.

Enrollment logs now use Spring Boot Logstash JSON plus SLF4J key/value fields. Alloy tails the JSON log file and forwards entries to Loki. Grafana supports both **Loki → Tempo** and **Tempo → Loki** navigation without using high-cardinality trace IDs as Loki stream labels.

The observability phase also defines a `99%` availability SLO, a `95% ≤ 500 ms` latency objective backed by an explicit `500ms` histogram bucket, error-budget and burn-rate panels, fast/slow multi-window burn alerts, and a verified Grafana webhook notification path.

Detailed theory, configuration, PromQL, tracing, centralized logging, SLOs and alerting: [OBSERVABILITY.md](docs/OBSERVABILITY.md)

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
* OAuth2 / OpenID Connect
* JWT authentication and validation
* Keycloak
* Role-based access control
* Testing
* JUnit 5 unit testing
* Mockito mocks, stubbing, verification and ArgumentCaptor
* Integration testing with Spring test slices
* Testcontainers with real MongoDB
* Observability
* Micrometer and Spring Boot Actuator metrics
* Prometheus and PromQL
* Grafana dashboards
* HTTP latency histograms and percentiles
* JVM CPU, heap and GC metrics
* OpenTelemetry distributed tracing with Tempo
* Structured JSON logging
* Grafana Alloy and Loki centralized logs
* Bidirectional logs ↔ traces correlation
* Scalability
* System design

---

# 👨‍💻 Author

**Rubén Marín**

Backend Java Developer

Technologies, architecture patterns and practices explored in this project include:


`Java 21` · `Spring Boot 4.1` · `Spring Web` · `RestClient` · `Spring Boot Actuator` · `Jakarta Bean Validation` · `Spring Data JPA` · `Hibernate` · `PostgreSQL` · `Spring Data MongoDB` · `MongoDB` · `MongoTemplate` · `Spring Cloud Circuit Breaker` · `Spring Kafka` · `Apache Kafka` · `KRaft` · `Docker` · `Docker Compose` · `Trivy` · `GitHub Actions` · `GitHub Container Registry (GHCR)` · `Kubernetes` · `Helm` · `Microservices` · `Event-Driven Architecture` · `DDD` · `Bounded Contexts` · `Context Mapping` · `Hexagonal Architecture` · `Ports and Adapters` · `Clean Architecture` · `API-first` · `OpenAPI 3.0.3` · `OpenAPI Generator 7.15.0` · `Spring Security` · `OAuth2 Resource Server` · `OpenID Connect` · `JWT` · `Keycloak` · `RBAC` · `JUnit 5` · `Mockito` · `Testcontainers` · `Micrometer` · `OpenTelemetry` · `Prometheus` · `PromQL` · `Grafana` · `Tempo` · `Loki` · `Grafana Alloy` · `DataMongoTest`
