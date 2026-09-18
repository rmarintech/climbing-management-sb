# Project Roadmap

This file is the **single source of truth for project progress**.

Do not duplicate these checkboxes in the technical learning documents.

After a milestone:

1. Mark it `[x]` here.
2. Add theory/examples only to the relevant topic document.
3. Update `README.md` only if the major phase or tech stack changes.

---

# ✅ Completed

## Java / Spring Boot

- [x] Spring Boot project setup
- [x] Java 21
- [x] REST API
- [x] Layered architecture
- [x] DTOs / Records
- [x] Bean Validation
- [x] Global Exception Handling
- [x] SLF4J
- [x] Actuator

## PostgreSQL / JPA / Hibernate

- [x] PostgreSQL
- [x] Spring Data JPA
- [x] Hibernate
- [x] CRUD
- [x] Derived Queries
- [x] JPQL
- [x] `@Transactional`
- [x] `REQUIRED`
- [x] `REQUIRES_NEW`
- [x] Rollback rules
- [x] Persistence Context
- [x] Dirty Checking
- [x] Optimistic Locking
- [x] Pessimistic Locking

## MongoDB

- [x] Spring Data MongoDB
- [x] MongoRepository
- [x] Derived Queries
- [x] Custom `@Query`
- [x] Operators
- [x] Text search
- [x] Pagination
- [x] Sorting
- [x] DTOs
- [x] Validation
- [x] Indexes
- [x] Compound indexes
- [x] `explain()`
- [x] MongoTemplate
- [x] Dynamic Queries
- [x] Aggregation
- [x] Aggregation DTO mapping
- [x] `$lookup`
- [x] Embedded vs referenced modelling
- [x] Replica set
- [x] Transactions
- [x] Rollback
- [x] Multi-collection transactions
- [x] PostgreSQL comparison

## Docker

- [x] Fundamentals
- [x] Images / Containers
- [x] Dockerfile
- [x] Layers / Build cache
- [x] Ports
- [x] Environment variables
- [x] Volumes
- [x] Networks
- [x] PostgreSQL container
- [x] MongoDB container
- [x] Docker Compose
- [x] Multi-container app
- [x] Service discovery
- [x] Multi-stage build
- [x] Alpine runtime
- [x] Non-root user
- [x] Read-only filesystem
- [x] tmpfs
- [x] Secrets
- [x] Docker Spring profile
- [x] Healthchecks
- [x] Troubleshooting
- [x] DNS troubleshooting
- [x] Trivy
- [x] Vulnerability remediation

## CI/CD

- [x] GitHub Actions
- [x] Service containers
- [x] Maven CI
- [x] Automated tests
- [x] JAR artifact
- [x] Docker image build
- [x] GHCR
- [x] Image publishing
- [x] Immutable SHA tags
- [x] Continuous Delivery
- [x] Production environment
- [x] Approval gate
- [x] Build once, deploy many

## Kubernetes

- [x] Cluster
- [x] Pods
- [x] Deployments
- [x] ReplicaSets
- [x] Services
- [x] ClusterIP
- [x] DNS / service discovery
- [x] ConfigMaps
- [x] Secrets
- [x] Startup probes
- [x] Readiness probes
- [x] Liveness probes
- [x] Rolling updates
- [x] Multiple replicas
- [x] EndpointSlices
- [x] PersistentVolumes
- [x] PersistentVolumeClaims
- [x] StorageClasses
- [x] PostgreSQL persistence
- [x] Resource requests
- [x] Resource limits
- [x] Metrics Server
- [x] `kubectl top`
- [x] HPA
- [x] CPU scaling
- [x] HPA load testing
- [x] Ingress
- [x] NGINX Ingress Controller
- [x] Local Ingress testing
- [x] Namespaces

## Helm

- [x] Fundamentals
- [x] Chart structure
- [x] `Chart.yaml`
- [x] `values.yaml`
- [x] Templates
- [x] `helm lint`
- [x] `helm template`
- [x] Releases
- [x] Install
- [x] Upgrade
- [x] History
- [x] Rollback
- [x] Uninstall
- [x] Namespace-aware deployment

## Microservices

- [x] Monolith vs microservices
- [x] Identify service boundaries
- [x] Define service responsibilities
- [x] Extract first service
- [x] Independent runtime
- [x] Independent persistence
- [x] Database-per-service ownership
- [x] Inter-service REST communication
- [x] Remote dependency failure handling
- [x] Failure isolation
- [x] Timeouts
- [x] Retries
- [x] Circuit breakers
- [x] Resilience and automatic recovery
- [x] Distributed-system trade-offs
- [x] Independent containerization
- [x] Independent Kubernetes deployment
- [x] Independent Docker images
- [x] Independent GHCR packages
- [x] Independent deployment

---

## Event-Driven Architecture / Kafka

- [x] Synchronous vs asynchronous
- [x] Apache Kafka
- [x] KRaft
- [x] Topics / partitions / offsets
- [x] Producers
- [x] Consumers
- [x] Consumer groups
- [x] Committed offsets and lag
- [x] Partition assignment
- [x] Rebalancing and consumer failover
- [x] Consumer parallelism
- [x] `ConsumerRecord` metadata
- [x] Event contracts
- [x] Event type and version
- [x] Schema evolution
- [x] Forward / backward compatibility
- [x] Breaking contract experiment
- [x] `ErrorHandlingDeserializer`
- [x] Retry strategy
- [x] Fixed backoff
- [x] Dead Letter Topic
- [x] Poison-pill handling
- [x] Retry diagnostics
- [x] Idempotency
- [x] EventId deduplication
- [x] MongoDB processed-event persistence
- [x] Duplicate skipping

---

# 🚧 Current — Architecture

## DDD / Hexagonal Architecture

- [ ] DDD
  - [x] DDD fundamentals
  - [x] Domain model separated from persistence model
  - [x] Entity identity
  - [x] Value Objects
  - [x] Aggregate / Aggregate Root fundamentals
  - [x] Domain invariants and behavior
  - [x] Framework-free domain model
  - [x] Pure domain unit tests
  - [x] Domain logic vs application orchestration
- [ ] Bounded Contexts
  - [x] Identify Course and Enrollment as separate business contexts
  - [x] Formalize context boundaries and responsibilities
- [ ] Hexagonal Architecture
  - [x] Application layer introduced
  - [x] Inbound port / use-case concept
  - [x] Outbound port concept
  - [x] Dependency inversion fundamentals
  - [x] Framework-free application service
  - [x] Application service unit tests with fake ports
  - [x] Outbound persistence adapter
  - [x] Domain → MongoDB persistence mapping
  - [x] Outbound Course REST adapter
  - [x] Spring wiring / dependency configuration
  - [x] Refactor POST REST flow into inbound adapter
  - [x] End-to-end Create Enrollment validation through adapters
  - [x] Read-side outbound port
  - [x] MongoDB → Domain rehydration
  - [x] Read-side application service
  - [x] Spring wiring for read use case
  - [x] Migrate `GET /enrollments`
  - [x] End-to-end read-side validation with persisted status
- [ ] Ports and Adapters
  - [x] `CreateEnrollmentUseCase`
  - [x] `SaveEnrollmentPort`
  - [x] `CourseExistsPort`
  - [x] MongoDB adapter
  - [x] Course REST adapter
  - [x] REST inbound adapter migration for POST
  - [x] `FindEnrollmentsUseCase`
  - [x] `FindEnrollmentsPort`
  - [x] Read-side MongoDB adapter
  - [x] REST inbound adapter migration for GET
- [x] Clean Architecture
- [ ] API-first design

---

# 🔜 Security

- [ ] Spring Security
- [ ] Authentication
- [ ] Authorization
- [ ] JWT
- [ ] RBAC

---

# 🔜 Testing

- [ ] Unit tests
- [ ] Mockito
- [ ] Integration tests
- [ ] Testcontainers
- [ ] Repository tests
- [ ] REST API tests

---

# 🔜 Advanced Backend Engineering

- [ ] API versioning
- [ ] Distributed tracing
- [ ] Observability
- [ ] Performance
- [ ] Scalability
- [ ] System design

---

# 🎯 Interview Preparation

- [ ] Senior Java
- [ ] Spring Boot
- [ ] JPA / Hibernate
- [ ] PostgreSQL
- [ ] MongoDB
- [ ] Docker
- [ ] CI/CD
- [ ] Kubernetes
- [ ] Helm
- [ ] Microservices
- [ ] Kafka
- [ ] System Design

---

# Current Position

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
Distributed-system trade-offs   ✅
        ↓
Independent containerization    ✅
        ↓
Independent K8s deployment      ✅
        ↓
Independent GHCR images         ✅
        ↓
Independent deployment          ✅
        ↓
Microservices                   ✅ 
        ↓
Kafka broker / KRaft            ✅
        ↓
Topics / partitions / offsets   ✅
        ↓
Spring producer                 ✅
        ↓
Spring consumer                 ✅
        ↓
Consumer groups                 ✅
        ↓
Partition assignment            ✅
        ↓
Rebalancing / failover          ✅
        ↓
Consumer parallelism            ✅
        ↓
Event contracts                 ✅
        ↓
Schema evolution                ✅
        ↓
Retry / DLT                     ✅
        ↓
Idempotency                     ✅
        ↓
Kafka / Event-Driven            ✅ 
        ↓
DDD fundamentals                ✅
        ↓
Entity / Value Objects          ✅
        ↓
Aggregate Root / invariants     ✅
        ↓
Pure domain tests               ✅
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
Read-side port / service         ✅
        ↓
Mongo → Domain rehydration      ✅
        ↓
GET inbound adapter             ✅
        ↓
Read-side E2E validation        ✅
        ↓
DDD / Hexagonal                 🚧 CURRENT
        ↓
Security                        ⏳
        ↓
Testing                         ⏳
        ↓
Advanced Backend Engineering    ⏳
        ↓
System Design                   ⏳
```

---

[Back to README](../README.md)
