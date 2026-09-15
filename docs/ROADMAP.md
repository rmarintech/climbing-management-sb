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

# 🚧 Current — Event-Driven Architecture

- [x] Synchronous vs asynchronous
- [x] Apache Kafka
- [x] Producers
- [x] Consumers
- [ ] Consumer groups
- [ ] Event contracts
- [ ] Retry strategy
- [ ] Idempotency

---

# 🔜 Architecture

- [ ] DDD
- [ ] Bounded Contexts
- [ ] Hexagonal Architecture
- [ ] Ports and Adapters
- [ ] Clean Architecture
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
Microservices                   ✅ COMPLETE
        ↓
Kafka broker / KRaft            ✅
        ↓
Topics / partitions / offsets   ✅
        ↓
Spring producer                 ✅
        ↓
Spring consumer                 ✅
        ↓
Consumer groups                 🚧 CURRENT
        ↓
Event contracts                 ⏳
        ↓
Retry strategy                  ⏳
        ↓
Idempotency                     ⏳
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

---

[Back to README](../README.md)
