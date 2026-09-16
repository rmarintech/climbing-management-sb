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
EnrollmentMongoDocument
├── String id
├── Long courseId
└── String studentName
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

At this learning stage, `CreateEnrollmentService` deliberately has no `@Service`.

This makes the application core easy to understand and test before Spring wiring is introduced.

Spring will later connect the ports and adapters at the edge.

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

# REST Controller as Future Inbound Adapter

Eventually the Enrollment controller should become a pure inbound adapter:

```text
HTTP request
      ↓
map to CreateEnrollmentCommand
      ↓
CreateEnrollmentUseCase
      ↓
map result to HTTP response
```

The controller should not own the business workflow.

---

# Course REST Client as Future Outbound Adapter

The existing resilient Course integration already has:

```text
RestClient
timeouts
retries
circuit breaker
error mapping
```

Hexagonal refactoring does not throw that away.

It will be placed behind:

```text
CourseExistsPort
```

so the application core does not depend on HTTP details.

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

# Target Adapter Shape

Target direction:

```text
Enrollment Service
├── domain
├── application
└── adapter
    ├── in
    │   ├── rest
    │   └── kafka
    └── out
        ├── persistence
        │   └── mongodb
        └── course
            └── rest
```

This is the target architecture, not all of it exists yet.

---

# Next Step — Mongo Persistence Adapter

The next implementation is:

```text
SaveEnrollmentPort
        ↑
MongoEnrollmentAdapter
        ↓
existing Mongo repository
        ↓
MongoDB
```

The adapter will map between:

```text
Domain Enrollment
```

and:

```text
EnrollmentMongoDocument
```

This is where the earlier domain/persistence separation becomes concrete.

---

# Mapping Belongs at the Boundary

Example direction:

```text
Domain
EnrollmentId("abc")
CourseId(10)
StudentName("Rubén")

      ↓ mapping

Mongo document
id = "abc"
courseId = 10
studentName = "Rubén"
```

The adapter absorbs the technical representation difference.

The domain does not need to become a Mongo document.

---

# Interview-Level Summary

A concise DDD explanation:

> DDD models software around business concepts, rules, language, and boundaries. Entities are identity-based, Value Objects are value-based, Aggregates define consistency boundaries, and Aggregate Roots protect invariants.

A concise Hexagonal explanation:

> Hexagonal Architecture isolates the domain and application core from infrastructure. External actors enter through inbound ports, while the application accesses external systems through outbound ports. Adapters implement technical details such as REST or MongoDB, so dependency direction points toward the core.

---

# Current Learning Position

The domain and application core are now established.

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
```

Current next step:

```text
SaveEnrollmentPort
        ↑
MongoEnrollmentAdapter
        ↓
Mongo persistence
```

Then:

```text
CourseExistsPort
        ↑
Course REST Adapter
```

Then:

```text
Spring wiring
      ↓
REST inbound adapter migration
      ↓
full hexagonal request flow
```

For milestone status, use [`ROADMAP.md`](ROADMAP.md).

---

[Back to README](../README.md)
