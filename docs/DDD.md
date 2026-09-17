# Domain-Driven Design / Hexagonal Architecture

This document contains the theory, architecture notes, implementation decisions, examples, and learning conclusions for the **DDD / Hexagonal Architecture** phase of the Climbing Management project.

Progress tracking belongs in [`ROADMAP.md`](ROADMAP.md).

The implementation is intentionally incremental. The existing Enrollment Service remains operational while a framework-free domain and application core are introduced alongside the current Spring / MongoDB code.

---

# Why This Phase Exists

The project already has a working layered architecture:

```text
REST Controller
      ↓
Service
      ↓
Repository
      ↓
MongoDB
```

That architecture is valid and common.

The purpose of this phase is to learn what changes when the business model becomes the center of the architecture rather than the framework or persistence technology.

Target direction:

```text
                 External world
                       ↓
                 Inbound Adapter
                       ↓
                  Inbound Port
                       ↓
               Application Service
                       │
              ┌────────┴────────┐
              ↓                 ↓
           Domain          Outbound Ports
                                │
                      ┌─────────┴─────────┐
                      ↓                   ↓
                Mongo Adapter       Course REST Adapter
                      ↓                   ↓
                   MongoDB           Course Service
```

The core dependency rule is:

```text
Infrastructure
      ↓
depends on
      ↓
Application / Domain
```

not the reverse.

---

# DDD Is Not Package Naming

DDD means **Domain-Driven Design**.

It is not simply creating a `domain` package. The central idea is to model software around business concepts, rules, language, and boundaries.

For this project:

```text
Climbing Management
├── Course
└── Enrollment
```

DDD gives us vocabulary to reason about those concepts explicitly.

---

# Bounded Contexts

The previous microservices phase already exposed two natural business boundaries:

```text
Course Context
      └── Course Service

Enrollment Context
      └── Enrollment Service
```

A Bounded Context defines a boundary inside which a model and its language have one clear meaning.

Important:

```text
Bounded Context ≠ automatically one microservice
```

A Bounded Context is a domain/model boundary. A microservice is a runtime/deployment boundary. They often align, but they are not the same concept.

---

# Domain Model vs Persistence Model

A domain object answers:

> What is this concept in the business?

A persistence object answers:

> How do I store this data?

Current direction:

```text
Domain
Enrollment
├── EnrollmentId
├── CourseId
├── StudentName
└── EnrollmentStatus
```

versus:

```text
Mongo persistence
EnrollmentDocument
├── String id
├── Long courseId
├── String studentName
└── String status
```

They can look similar without being the same class.

---

# Entity

An **Entity** is defined primarily by identity.

Example:

```text
Enrollment A
id = enrollment-1

Enrollment B
id = enrollment-2
```

Even if both have the same Course and student values, they are different Enrollments because their identities differ.

The current domain Entity is `Enrollment`.

---

# Value Object

A **Value Object** is defined by its value rather than by an independent identity.

Current Value Objects:

```text
EnrollmentId
CourseId
StudentName
```

Java records are a natural fit because they provide value-based equality.

Example:

```java
new CourseId(10L).equals(new CourseId(10L))
```

evaluates to `true`.

---

# CourseId

`CourseId` replaces a weak primitive:

```java
Long courseId;
```

with a domain type that can protect its invariants:

```java
public record CourseId(Long value) {

    public CourseId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Course id cannot be null"
            );
        }

        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Course id must be positive"
            );
        }
    }
}
```

The domain now guarantees that a constructed `CourseId` is valid.

---

# EnrollmentId

`EnrollmentId` gives the Entity's identity an explicit domain type instead of using a raw `String`.

Conceptually:

```text
Enrollment
    ↓
EnrollmentId
```

rather than:

```text
Enrollment
    ↓
arbitrary String
```

---

# StudentName

`StudentName` replaces another primitive:

```java
String studentName;
```

with a Value Object that owns validation and normalization.

Example:

```java
public record StudentName(String value) {

    public StudentName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Student name cannot be null or blank"
            );
        }

        value = value.trim();
    }
}
```

If `StudentName` exists, it is not null, not blank, and already normalized.

---

# Primitive Obsession

Using generic primitives everywhere weakens the domain model.

Example:

```java
void method(Long courseId, Long studentId, Long enrollmentId)
```

Java only sees three `Long` values.

Domain types:

```text
CourseId
StudentId
EnrollmentId
```

make intent explicit and reduce accidental mixing.

---

# Aggregate and Aggregate Root

An **Aggregate** is a consistency boundary around related domain objects.

One Entity is the **Aggregate Root**.

Current model:

```text
Enrollment Aggregate
└── Enrollment                  Entity + Aggregate Root
    ├── EnrollmentId            Value Object
    ├── CourseId                Value Object
    ├── StudentName             Value Object
    └── EnrollmentStatus        Domain concept
```

A single Entity can be a complete aggregate.

---

# Why Course Is Not Inside Enrollment

Enrollment refers to a Course but does not own the Course lifecycle.

So:

```text
Enrollment
    ↓
CourseId
```

is preferred over embedding a full Course object from another context.

This preserves the Course/Enrollment boundary.

---

# Domain Invariants

A domain invariant is a rule that must always hold for a valid model.

The learning lifecycle is:

```text
new Enrollment
      ↓
   PENDING
     ├── confirm() → CONFIRMED
     └── cancel()  → CANCELLED
```

Invalid transitions are rejected.

The constructor also establishes the initial invariant:

```java
this.status = EnrollmentStatus.PENDING;
```

The first domain test exposed this directly when `status` was accidentally left `null`.

---

# Behavior-Rich Domain Model

Instead of exposing:

```java
setStatus(...)
```

the Aggregate Root exposes business behavior:

```java
enrollment.confirm();
enrollment.cancel();
```

This keeps transition rules inside the model rather than scattering them across services.

---

# Framework-Free Domain

The new domain deliberately contains no:

```text
@Document
@Entity
@Service
@Repository
MongoRepository
RestClient
KafkaTemplate
ResponseEntity
```

The domain is pure Java.

That makes it independent of Spring, MongoDB, Kafka, HTTP, Docker, and Kubernetes.

---

# Pure Domain Unit Tests

The domain tests instantiate the model directly:

```java
Enrollment enrollment = new Enrollment(
        new EnrollmentId("enrollment-1"),
        new CourseId(10L),
        new StudentName("Rubén")
);
```

No Spring context is needed.

No MongoDB is needed.

No HTTP or Kafka is needed.

Tests currently cover the initial state, valid/invalid status transitions, and Value Object validation.

---

# Domain Logic vs Application Orchestration

This distinction is central.

Domain logic:

```text
CourseId must be positive
StudentName cannot be blank
Enrollment starts PENDING
Enrollment.confirm()
Enrollment.cancel()
```

Application orchestration:

```text
receive create-enrollment input
validate Course externally
construct domain types
create Enrollment
save Enrollment
return result
```

The latter belongs in the application layer.

---

# Application Layer

The application layer coordinates use cases.

Current direction:

```text
application
├── port
│   ├── in
│   └── out
└── service
```

The application layer knows the domain but should not know MongoDB or HTTP implementation details.

---

# Inbound Port / Use Case

An inbound port describes what the application offers.

Current use case:

```java
public interface CreateEnrollmentUseCase {

    Enrollment createEnrollment(
            CreateEnrollmentCommand command
    );
}
```

This is technically an inbound port. It is named `UseCase` because it represents a business action that an external actor can invoke.

---

# CreateEnrollmentCommand

Input to the use case is represented by:

```java
public record CreateEnrollmentCommand(
        Long courseId,
        String studentName
) {
}
```

The application converts transport-friendly values into domain types:

```text
Long   → CourseId
String → StudentName
```

---

# Outbound Ports

Outbound ports describe capabilities the application needs from outside the core.

Current ports:

```text
SaveEnrollmentPort
CourseExistsPort
```

The application does not ask directly for MongoDB or RestClient.

It asks for capabilities.

---

# SaveEnrollmentPort

```java
public interface SaveEnrollmentPort {

    Enrollment save(Enrollment enrollment);
}
```

The contract says:

> I need something that can persist an Enrollment.

It says nothing about MongoDB.

Target implementation:

```text
SaveEnrollmentPort
        ↑
MongoEnrollmentAdapter
        ↓
MongoRepository
        ↓
MongoDB
```

---

# CourseExistsPort

```java
public interface CourseExistsPort {

    boolean existsById(CourseId courseId);
}
```

The contract says:

> I need a way to determine whether this Course exists.

It does not mention HTTP, URLs, RestClient, retries, or circuit breakers.

Those are adapter concerns.

---

# Why Inbound Is Called UseCase and Outbound Is Called Port

Both are ports.

The naming convention emphasizes direction:

```text
Inbound port
    = what the application offers

Outbound port
    = what the application requires
```

So:

```text
CreateEnrollmentUseCase
```

is an inbound port.

And:

```text
SaveEnrollmentPort
CourseExistsPort
```

are outbound ports.

Other literature may call them:

```text
Primary Port
Secondary Port
```

respectively.

---

# CreateEnrollmentService

`CreateEnrollmentService` implements the inbound use case.

Conceptually:

```text
CreateEnrollmentUseCase
        ↑
CreateEnrollmentService
        ├── CourseExistsPort
        └── SaveEnrollmentPort
```

Its workflow is:

```text
CreateEnrollmentCommand
        ↓
create CourseId
        ↓
CourseExistsPort
        ↓
Course exists?
        ├── no → fail
        └── yes
              ↓
       create EnrollmentId
              ↓
       create StudentName
              ↓
       create Enrollment
              ↓
       SaveEnrollmentPort
              ↓
       return Enrollment
```

This is application orchestration, not domain behavior.

---

# Framework-Free Application Service

`CreateEnrollmentService` deliberately has no `@Service`.

This keeps the application core independent from Spring and allows it to be instantiated directly in unit tests.

Spring wiring is now provided outside the application core through a composition-root configuration class using `@Configuration` and `@Bean`.

---

# Application Service Unit Tests

The application service depends only on interfaces.

Tests can therefore provide tiny fake adapters:

```java
CourseExistsPort courseExistsPort =
        courseId -> true;

SaveEnrollmentPort saveEnrollmentPort =
        enrollment -> enrollment;
```

Production uses real adapters.

Tests use fakes.

Same application service, different adapters.

This is one of the clearest practical benefits of Hexagonal Architecture.

---

# Success and Failure Paths

Success:

```text
Course exists
      ↓
create Enrollment
      ↓
save
      ↓
return PENDING Enrollment
```

Failure:

```text
Course missing
      ↓
fail use case
      ↓
SaveEnrollmentPort must not be called
```

The unit test uses a failing fake save port to prove persistence is not invoked after Course validation fails.

---

# Dependency Inversion

Without inversion:

```text
CreateEnrollmentService
      ↓
MongoRepository
```

The core depends on infrastructure.

With inversion:

```text
CreateEnrollmentService
      ↓
SaveEnrollmentPort
      ↑
MongoEnrollmentAdapter
      ↓
MongoRepository
```

Infrastructure depends on a core-owned abstraction.

That is Dependency Inversion applied at architecture level.

---

# Ports Do Not Mean Interface Everything

Hexagonal Architecture does not require an interface for every class.

Ports represent meaningful boundaries.

Good examples here:

```text
CreateEnrollmentUseCase
SaveEnrollmentPort
CourseExistsPort
```

Internal helpers do not automatically need interfaces.

---

# Domain Service vs Application Service

Application Service:

```text
coordinates a use case
```

Current example:

```text
CreateEnrollmentService
```

Domain Service:

```text
contains domain logic that does not naturally belong to one Entity or Value Object
```

The project does not currently need a Domain Service for Enrollment.

Do not invent one just to satisfy a pattern.

---

# Repository in DDD vs Spring Data Repository

DDD Repository means a domain-oriented abstraction for accessing aggregates.

Spring Data Repository means a framework abstraction such as:

```java
MongoRepository<...>
```

They are related but not identical concepts.

The current architecture keeps the Spring Data repository on the infrastructure side and exposes application needs through `SaveEnrollmentPort`.

---

# REST Controller as Inbound Adapter

The `POST /enrollments` flow has now been migrated to the new inbound port.

The controller performs only boundary responsibilities:

```text
HTTP request
      ↓
map to CreateEnrollmentCommand
      ↓
CreateEnrollmentUseCase
      ↓
map Domain Enrollment
      ↓
EnrollmentResponse
      ↓
HTTP response
```

The controller no longer owns Course validation or persistence workflow for POST.

The old `GET /enrollments` path is still temporarily using the previous service and will be migrated separately.

---

# EnrollmentResponse as REST DTO

The project now uses a dedicated HTTP response model:

```text
EnrollmentResponse
├── id
├── courseId
├── studentName
└── status
```

This makes the API boundary explicit:

```text
HTTP representation
        ≠
Domain model
        ≠
Persistence model
```

The domain can evolve according to business rules without being forced to match the JSON contract or MongoDB document shape exactly.

---

# Course REST Adapter

The resilient Course integration is now behind the outbound port:

```text
CreateEnrollmentService
      ↓
CourseExistsPort
      ↑
CourseRestAdapter
      ↓
CircuitBreaker
      ↓
CourseClient
      ↓
RestClient
      ↓
Course Service
```

`CourseRestAdapter` converts the domain `CourseId` into the primitive `Long` required by the existing client while keeping HTTP, retries, and circuit-breaker concerns outside the application core.

---

# Kafka and Hexagonal Architecture

Kafka fits the same model.

A Kafka listener can be an inbound adapter:

```text
Kafka
  ↓
Kafka listener adapter
  ↓
application use case
```

A Kafka producer can be an outbound adapter:

```text
application
  ↓
PublishEventPort
  ↓
Kafka producer adapter
  ↓
Kafka
```

The core should depend on a business capability, not directly on `KafkaTemplate`.

---

# DDD, Hexagonal Architecture and Clean Architecture

They overlap but are not identical.

A practical distinction:

```text
DDD
    models the business

Hexagonal Architecture
    isolates the core with ports and adapters

Clean Architecture
    emphasizes dependency direction and concentric boundaries
```

They work well together.

---

# Current Package Shape

Current learning structure:

```text
com.rubenmarin.enrollmentservice
├── domain
│   └── model
│       ├── Enrollment
│       ├── EnrollmentId
│       ├── CourseId
│       ├── StudentName
│       └── EnrollmentStatus
│
└── application
    ├── port
    │   ├── in
    │   │   ├── CreateEnrollmentCommand
    │   │   └── CreateEnrollmentUseCase
    │   └── out
    │       ├── SaveEnrollmentPort
    │       └── CourseExistsPort
    └── service
        └── CreateEnrollmentService
```

The existing Spring/Mongo classes remain alongside this while adapters are introduced incrementally.

---

# Current Adapter Shape

The Enrollment Service now has the first real Hexagonal adapters:

```text
Enrollment Service
├── domain
│   └── model
│
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   └── service
│
├── adapter
│   ├── in
│   │   └── rest
│   │       └── EnrollmentResponse
│   │
│   └── out
│       ├── persistence
│       │   └── mongodb
│       │       └── MongoEnrollmentAdapter
│       │
│       └── course
│           └── rest
│               └── CourseRestAdapter
│
└── configuration
    └── EnrollmentApplicationConfiguration
```

The `POST /enrollments` path now uses this architecture end-to-end. The read side is still pending migration.

---

# Mongo Persistence Adapter

`MongoEnrollmentAdapter` now implements `SaveEnrollmentPort`:

```text
CreateEnrollmentService
      ↓
SaveEnrollmentPort
      ↑
MongoEnrollmentAdapter
      ↓
EnrollmentRepository
      ↓
MongoDB
```

Its responsibility is to translate the domain aggregate into the MongoDB persistence model.

Current mapping:

```text
Domain                          MongoDB

EnrollmentId("abc")     →       "abc"
CourseId(10)            →       10
StudentName("Rubén")    →       "Rubén"
EnrollmentStatus.PENDING→       "PENDING"
```

The domain does not contain MongoDB annotations or Spring Data types.

---

# Adapter Unit Testing

The real outbound adapters are unit-tested independently.

For `MongoEnrollmentAdapter`, Mockito is used to mock the Spring Data repository, and `ArgumentCaptor<EnrollmentDocument>` captures the object passed to `save(...)`.

That lets the test verify the Domain → Mongo mapping without requiring a running MongoDB instance.

`CourseRestAdapter` is also tested with mocked `CourseClient`, `CircuitBreakerFactory`, and `CircuitBreaker`. The mock circuit breaker is configured to execute the supplied `Supplier`, allowing the test to verify that the underlying client receives the primitive Course id.

---

# Spring Composition Root

The application service remains framework-free.

Spring creates it through:

```text
EnrollmentApplicationConfiguration
        ↓
@Configuration
        ↓
@Bean CreateEnrollmentUseCase
        ↓
new CreateEnrollmentService(
    CourseExistsPort,
    SaveEnrollmentPort
)
```

This distinguishes two related concepts:

```text
Dependency Inversion
    ↓
application depends on ports

Dependency Injection
    ↓
Spring provides adapter implementations
```

---

# End-to-End Create Enrollment Validation

The complete POST flow has been executed successfully:

```text
POST /enrollments
      ↓
EnrollmentController
      ↓
CreateEnrollmentCommand
      ↓
CreateEnrollmentUseCase
      ↓
CreateEnrollmentService
      │
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

The application generates the `EnrollmentId` as a UUID before persistence.

MongoDB stores the corresponding document with `courseId`, `studentName`, and `status = "PENDING"`. The REST response is mapped through `EnrollmentResponse` so the status is exposed without returning the domain entity directly.

---

# Current Next Step — Read Side and Rehydration

The write side is now migrated.

The next architectural problem is the read side:

```text
MongoDB
   ↓
EnrollmentDocument
   ↓
Mongo adapter
   ↓
Domain Enrollment
   ↓
GET /enrollments
```

This introduces an important DDD concept: **rehydration**.

A new Enrollment always starts as `PENDING`, but an Enrollment loaded from persistence may already be `CONFIRMED` or `CANCELLED`. Reading from MongoDB therefore cannot simply use the normal creation path if that path always establishes a new `PENDING` aggregate.

The domain will need a safe reconstruction path that restores persisted state while preserving invariants.

---

# Interview-Level Summary

A concise DDD explanation:

> DDD models software around business concepts, rules, language, and boundaries. Entities are identity-based, Value Objects are value-based, Aggregates define consistency boundaries, and Aggregate Roots protect invariants.

A concise Hexagonal explanation:

> Hexagonal Architecture isolates the domain and application core from infrastructure. External actors enter through inbound ports, while the application accesses external systems through outbound ports. Adapters implement technical details such as REST or MongoDB, so dependency direction points toward the core.

---

# Current Learning Position

The **Create Enrollment** use case is now fully wired through the first Hexagonal path.

Covered so far:

```text
DDD fundamentals
Entity identity
Value Objects
Aggregate Root
Domain invariants
Behavior-rich domain model
Framework-free domain tests
Application layer
Inbound use-case port
Outbound ports
Dependency inversion
Framework-free application service
Application service unit tests with fake ports
Mongo persistence adapter
Domain → MongoDB mapping
Course REST adapter
Adapter unit tests with Mockito
Spring composition root
POST REST inbound mapping
EnrollmentResponse REST DTO
End-to-end Create Enrollment validation
```

Current next step:

```text
MongoDB
   ↓
MongoDB → Domain rehydration
   ↓
read-side outbound port
   ↓
GET /enrollments migration
```

This is the point where the project will distinguish creating a new aggregate from rehydrating an existing aggregate with previously persisted state.

For milestone status, use [`ROADMAP.md`](ROADMAP.md).

---

[Back to README](../README.md)
