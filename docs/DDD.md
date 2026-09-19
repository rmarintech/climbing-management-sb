# Domain-Driven Design / Hexagonal / Clean / API-First Architecture

This document contains the theory, architecture notes, implementation decisions, examples, and learning conclusions for the **DDD / Hexagonal / Clean Architecture / API-first** phase of the Climbing Management project.

Progress tracking belongs in [`ROADMAP.md`](ROADMAP.md).

The implementation was introduced incrementally: the framework-free domain and application core were built alongside the original Spring/MongoDB flow, then the Enrollment POST and GET paths were migrated to the new ports-and-adapters structure.

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

The current project formalizes the responsibilities as:

```text
Course Context
├── owns Course
├── owns Course persistence
└── exposes Course-related contracts

Enrollment Context
├── owns Enrollment
├── owns Enrollment lifecycle/status
├── owns Enrollment persistence
└── refers to Course only through CourseId / external contracts
```

The Enrollment Service was checked for direct imports of the Course application's internal model (`CourseEntity`, `CourseRecord`, `CourseMongoDocument`, and Course application packages). No such dependencies were found.

This means the Enrollment Context does not share the upstream Course implementation model.

---

# Bounded Context vs Aggregate vs Microservice vs Database Boundary

These concepts are related but solve different problems:

```text
Bounded Context
    semantic/model boundary

Aggregate
    consistency boundary inside a domain model

Microservice
    runtime/deployment boundary

Database boundary
    data ownership boundary
```

In this project they align cleanly:

```text
Enrollment Context
      ↓
contains
      ↓
Enrollment Aggregate
      ↓
implemented by
      ↓
Enrollment Service
      ↓
owns
      ↓
Enrollment MongoDB data
```

This alignment is useful, but it is not a universal DDD rule. One Bounded Context does not automatically equal one microservice.

---

# Context Map — Course and Enrollment

The relationship is:

```text
UPSTREAM

Course Context
      │
      │ published REST / event contracts
      ▼
CourseRestAdapter / Kafka boundary
      │
      │ translation / isolation
      ▼
Enrollment Context

DOWNSTREAM
```

Course is **upstream** because it owns Course information that Enrollment needs.

Enrollment is **downstream** because it consumes that information.

The downstream context protects its own language through:

```text
CourseExistsPort
CourseId
CourseRestAdapter
```

rather than importing Course persistence or domain classes.

---

# Context Mapping Patterns

## Anti-Corruption Layer

The Enrollment side uses an ACL-like translation boundary:

```text
Course Context
      ↓
external HTTP representation
      ↓
CourseRestAdapter
      ↓
CourseExistsPort
      ↓
Enrollment language
```

The purpose is to stop external model changes from leaking into the Enrollment domain.

The important property is not simply "there is an adapter". The important property is that the boundary translates and protects the local model.

## Published Language

A Published Language is an explicit shared communication contract.

This project already demonstrates two examples:

```text
REST contract
Course Service → Enrollment Service
```

and:

```text
Kafka event contract
CourseCreatedEvent
```

The Kafka event contract is especially explicit because it has fields such as `eventType`, `eventVersion`, `sourceService`, `occurredAt`, and Course data.

The producer and consumer own separate Java representations while agreeing on the external message contract.

## Conformist

A Conformist downstream context accepts the upstream model largely as-is.

That is not what Enrollment currently does. It does not import Course internal classes; it keeps its own `CourseId` and local port abstractions.

## Customer / Supplier

Customer / Supplier describes the organizational relationship in which an upstream supplier intentionally serves downstream requirements.

Because this learning project owns both contexts, that relationship could be designed explicitly, but the strongest patterns demonstrated in code today are the published contracts and the protected Enrollment model.

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

Current outbound ports:

```text
SaveEnrollmentPort
FindEnrollmentsPort
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

Both Enrollment REST endpoints enter the application through inbound use cases, but the HTTP contract is now generated from OpenAPI.

Write side:

```text
POST /enrollments
      ↓
generated EnrollmentsApi
      ↓
generated EnrollmentRequest
      ↓
EnrollmentController
      ↓
CreateEnrollmentCommand
      ↓
CreateEnrollmentUseCase
      ↓
Domain Enrollment
      ↓
generated EnrollmentResponse
```

Read side:

```text
GET /enrollments
      ↓
generated EnrollmentsApi
      ↓
EnrollmentController
      ↓
FindEnrollmentsUseCase
      ↓
List<Domain Enrollment>
      ↓
generated EnrollmentResponse[]
```

`EnrollmentController` remains under `adapter/in/rest`, but the handwritten `EnrollmentRequest` and `EnrollmentResponse` classes were removed after the OpenAPI-generated models replaced them.

The controller now implements:

```java
EnrollmentsApi
```

The generated interface owns the HTTP mappings. The controller owns the implementation behavior and mapping between generated HTTP models and application/domain concepts.

This preserves the boundary:

```text
OpenAPI-generated HTTP contract
        ↓
REST adapter
        ↓
application
        ↓
domain
```

Generated OpenAPI types do not leak into the application or domain layers.

---

# OpenAPI-Generated REST Models

The external API models now come from `openapi/enrollment-api.yaml`.

Generated models:

```text
EnrollmentRequest
├── courseId
└── studentName

EnrollmentResponse
├── id
├── courseId
├── studentName
└── status

ErrorResponse
├── timestamp
├── message
└── status
```

The API boundary remains explicitly different from the domain and persistence models:

```text
OpenAPI / HTTP representation
        ≠
Domain model
        ≠
Persistence model
```

The generated request model also carries Bean Validation constraints derived from the contract.

For example:

```yaml
courseId:
  type: integer
  format: int64
  minimum: 1
```

generates a Java validation constraint equivalent to:

```java
@Min(1L)
```

With `@Valid` at the REST boundary, invalid requests can be rejected before they enter the application use case.

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

# FindEnrollmentsPort

The read side introduces another outbound capability:

```java
public interface FindEnrollmentsPort {

    List<Enrollment> findAll();
}
```

The application does not depend on `MongoRepository` or `EnrollmentDocument`. It only asks for a capability that can return Enrollments.

```text
FindEnrollmentsService
        ↓
FindEnrollmentsPort
        ↑
MongoEnrollmentAdapter
        ↓
EnrollmentRepository
        ↓
MongoDB
```

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

The Clean Architecture rule used here is:

```text
Source-code dependencies point inward.
```

Applied to the Enrollment Service:

```text
OUTSIDE

Spring / HTTP / MongoDB
        ↓
Adapters
        ↓
Application / Use Cases
        ↓
Domain

INSIDE
```

The runtime execution path can move outward through an outbound port:

```text
CreateEnrollmentService
        ↓
SaveEnrollmentPort
        ↓
MongoEnrollmentAdapter
        ↓
MongoDB
```

but the source-code dependency is inverted:

```text
CreateEnrollmentService
        ↓
SaveEnrollmentPort
        ↑
MongoEnrollmentAdapter implements the port
```

The application owns the abstraction; infrastructure supplies the implementation.

This connects three ideas learned in this phase:

```text
Dependency Inversion Principle
        ↓
core owns the abstraction

Hexagonal Architecture
        ↓
ports and adapters

Clean Architecture
        ↓
dependencies point inward
```

## Dependency Rule Verification

The actual source tree was checked directly.

The domain package had no references to Spring, MongoDB, REST DTOs, Kafka, or persistence documents.

The application package had no references to Spring, `MongoRepository`, `RestClient`, `KafkaTemplate`, or `EnrollmentDocument`.

The adapter package did contain imports of application ports and domain types, as expected.

So the verified direction is:

```text
domain      → infrastructure    NO
application → infrastructure    NO
adapters    → application       YES
adapters    → domain            YES
```

That is the intended Clean Architecture dependency rule in the real codebase.

---

# API-First Design

API-first changes the source of truth for the HTTP boundary.

Earlier:

```text
Controller code
      ↓
API behavior emerges from implementation
```

Current approach:

```text
OpenAPI contract
      ↓
generated Java contract
      ↓
controller implementation
```

The Enrollment API contract is stored at:

```text
services/enrollment-service/openapi/enrollment-api.yaml
```

The contract currently defines:

```text
GET /enrollments
└── 200 → EnrollmentResponse[]

POST /enrollments
├── request → EnrollmentRequest
├── 201 → EnrollmentResponse
├── 400 → ErrorResponse
├── 404 → ErrorResponse
└── 503 → ErrorResponse
```

## Reusable OpenAPI Schemas

The contract uses reusable `components/schemas` instead of repeating inline structures:

```text
EnrollmentRequest
EnrollmentResponse
ErrorResponse
```

This centralizes the public API representation.

The `EnrollmentResponse.status` contract explicitly allows:

```text
PENDING
CONFIRMED
CANCELLED
```

without coupling the OpenAPI document to the Java domain enum class.

## Contract-Driven Validation

Validation constraints are part of the API contract.

Examples:

```text
courseId   minimum = 1
studentName minLength = 1
```

OpenAPI Generator produces Bean Validation annotations from those constraints.

The tested runtime flow is:

```text
OpenAPI minimum: 1
        ↓
generated @Min(1L)
        ↓
@Valid
        ↓
Spring validation
        ↓
MethodArgumentNotValidException
        ↓
GlobalExceptionHandler
        ↓
generated ErrorResponse
        ↓
400 Bad Request
```

This was verified with a negative `courseId`.

## Contract Alignment Exposed Real Regressions

Writing the API contract exposed implementation behavior that no longer matched the intended external API after the Hexagonal refactor.

Missing Course initially produced:

```text
500 Internal Server Error
```

The application was corrected to use `CourseNotFoundException`, restoring:

```text
404 Not Found
```

An invalid Course id initially also produced:

```text
500 Internal Server Error
```

The REST boundary was corrected so invalid input produces:

```text
400 Bad Request
```

A real Course Service outage was tested and produced:

```text
503 Service Unavailable
```

All three error cases now use the same public `ErrorResponse` shape.

## Standardized Error Contract

The OpenAPI schema defines:

```text
ErrorResponse
├── timestamp
├── message
└── status
```

The handwritten Java `ErrorResponse` was removed.

`GlobalExceptionHandler` now returns the generated OpenAPI `ErrorResponse`, whose timestamp is represented as `OffsetDateTime`.

This removes duplicated definitions of the external error contract.

## Maven Contract Validation

The `openapi-generator-maven-plugin` validates the OpenAPI specification during the Maven lifecycle.

The validation was explicitly proven by temporarily changing:

```yaml
openapi: 3.0.3
```

to an invalid value.

The build failed with a `SpecValidationException`.

After restoring the valid specification:

```text
mvn clean verify
        ↓
BUILD SUCCESS
```

This proves the contract is an enforced build artifact rather than passive documentation.

## Generated Models

The build currently generates:

```text
target/generated-sources/openapi/src/main/java/
└── com/rubenmarin/enrollmentservice/api/generated/
    └── model/
        ├── EnrollmentRequest.java
        ├── EnrollmentResponse.java
        └── ErrorResponse.java
```

Generated source is kept under `target/generated-sources`; it is not copied into `src/main/java`.

Conceptually:

```text
OpenAPI YAML
      ↓
source of truth

generated Java
      ↓
derived build artifact
```

## Generated API Interface

The generator also creates:

```text
api/generated/api/EnrollmentsApi.java
```

The important generator options are:

```xml
<interfaceOnly>true</interfaceOnly>
<skipDefaultInterface>true</skipDefaultInterface>
```

`interfaceOnly=true` means the generator creates the API contract interface but does not create a competing controller implementation.

`skipDefaultInterface=true` means the generated interface does not provide fallback/default endpoint implementations. The real controller must implement the contract.

The generated interface contains the Spring MVC mappings for:

```text
POST /enrollments
GET  /enrollments
```

`EnrollmentController` now implements `EnrollmentsApi`.

So ownership is:

```text
OpenAPI YAML
      ↓
defines HTTP contract
      ↓
generated EnrollmentsApi
      ↓
EnrollmentController
      ↓
implements behavior
```

## Documentation Generation Options

The generator is configured with:

```xml
<documentationProvider>none</documentationProvider>
<annotationLibrary>none</annotationLibrary>
```

This avoids generating runtime Springdoc / Swagger documentation integration and avoids Swagger-specific Java annotations such as `@Schema`.

The OpenAPI YAML remains the contract source of truth without requiring additional documentation annotation dependencies in the generated Java code.

## API-First and Clean Architecture Together

API-first does not mean generated types should spread through the whole application.

The current dependency boundary remains:

```text
OpenAPI-generated models/interface
        ↓
adapter/in/rest
        ↓
application ports / commands
        ↓
domain
```

Verified design rule:

```text
domain        → no generated OpenAPI classes
application   → no generated OpenAPI classes
REST adapter  → generated OpenAPI classes
```

This keeps the API contract explicit while preserving the Hexagonal and Clean Architecture dependency rules.

## End-to-End Generated Contract Validation

After `EnrollmentController` implemented the generated `EnrollmentsApi`, both operations were tested successfully:

```text
POST /enrollments → 201 Created
GET  /enrollments → 200 OK
```

The created Enrollment appeared in the subsequent GET response.

This proves the complete API-first path:

```text
enrollment-api.yaml
        ↓
OpenAPI Generator
        ↓
EnrollmentsApi + generated models
        ↓
EnrollmentController
        ↓
application use cases
        ↓
domain
        ↓
MongoDB
```

---

# Current Package Shape

Current source structure:

```text
services/enrollment-service/
├── openapi/
│   └── enrollment-api.yaml
│
└── src/main/java/com/rubenmarin/enrollmentservice/
    ├── domain
    │   └── model
    │       ├── Enrollment
    │       ├── EnrollmentId
    │       ├── CourseId
    │       ├── StudentName
    │       └── EnrollmentStatus
    │
    ├── application
    │   ├── port
    │   │   ├── in
    │   │   │   ├── CreateEnrollmentCommand
    │   │   │   ├── CreateEnrollmentUseCase
    │   │   │   └── FindEnrollmentsUseCase
    │   │   └── out
    │   │       ├── SaveEnrollmentPort
    │   │       ├── FindEnrollmentsPort
    │   │       └── CourseExistsPort
    │   └── service
    │       ├── CreateEnrollmentService
    │       └── FindEnrollmentsService
    │
    ├── adapter
    │   ├── in
    │   │   └── rest
    │   │       └── EnrollmentController
    │   └── out
    │       ├── persistence
    │       │   └── mongodb
    │       │       └── MongoEnrollmentAdapter
    │       └── course
    │           └── rest
    │               └── CourseRestAdapter
    │
    └── configuration
        └── EnrollmentApplicationConfiguration
```

Generated API artifacts live under the build output:

```text
target/generated-sources/openapi/src/main/java/
└── com/rubenmarin/enrollmentservice/api/generated/
    ├── api
    │   └── EnrollmentsApi
    └── model
        ├── EnrollmentRequest
        ├── EnrollmentResponse
        └── ErrorResponse
```

The generated code is derived from `openapi/enrollment-api.yaml` and is not manually maintained.

---

# Current Adapter Shape

The current inbound REST side now includes a generated contract boundary:

```text
OpenAPI contract
      ↓
generated EnrollmentsApi
      ↓
EnrollmentController
      ↓
application use cases
```

The complete Enrollment Service shape is:

```text
Enrollment Service
├── openapi
│   └── enrollment-api.yaml
│
├── generated API contract
│   ├── EnrollmentsApi
│   ├── EnrollmentRequest
│   ├── EnrollmentResponse
│   └── ErrorResponse
│
├── adapter
│   ├── in
│   │   └── rest
│   │       └── EnrollmentController
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
├── application
├── domain
│
└── configuration
    └── EnrollmentApplicationConfiguration
```

Both `POST /enrollments` and `GET /enrollments` have been validated end-to-end through the generated API interface.

---

# Mongo Persistence Adapter

`MongoEnrollmentAdapter` now implements both `SaveEnrollmentPort` and `FindEnrollmentsPort`:

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

The read side performs the inverse mapping:

```text
MongoDB → Domain mapping

String id          → EnrollmentId
Long courseId      → CourseId
String studentName → StudentName
String status      → EnrollmentStatus
```

The status conversion uses:

```java
EnrollmentStatus.valueOf(enrollmentDocument.getStatus())
```

and the aggregate is restored through:

```java
Enrollment.rehydrate(...)
```

rather than the normal constructor used for new Enrollments.

---

# Adapter Unit Testing

The real outbound adapters are unit-tested independently.

For `MongoEnrollmentAdapter`, Mockito is used to mock the Spring Data repository, and `ArgumentCaptor<EnrollmentDocument>` captures the object passed to `save(...)`.

That lets the test verify the Domain → Mongo mapping without requiring a running MongoDB instance.

A second Mongo adapter test covers the reverse direction by mocking `EnrollmentRepository.findAll()`, returning an `EnrollmentDocument`, and asserting that the resulting domain `Enrollment` preserves its id, Course id, student name, and persisted status.

`CourseRestAdapter` is also tested with mocked `CourseClient`, `CircuitBreakerFactory`, and `CircuitBreaker`. The mock circuit breaker is configured to execute the supplied `Supplier`, allowing the test to verify that the underlying client receives the primitive Course id.

---

# Spring Composition Root

The domain and application layers remain framework-free.

Spring is intentionally allowed in the composition root because this is the assembly point where technical implementations are connected to framework-free application services.

Current wiring:

```text
EnrollmentApplicationConfiguration
        ↓
@Configuration
        │
        ├── @Bean CreateEnrollmentUseCase
        │       ↓
        │   new CreateEnrollmentService(
        │       CourseExistsPort,
        │       SaveEnrollmentPort
        │   )
        │
        └── @Bean FindEnrollmentsUseCase
                ↓
            new FindEnrollmentsService(
                FindEnrollmentsPort
            )
```

Both bean methods expose the inbound use-case interfaces rather than the concrete application-service types.

The resulting separation is:

```text
domain
    pure Java

application
    pure Java

adapters
    Spring / MongoDB / HTTP integrations

configuration
    Spring wiring / composition root
```

This distinguishes two related concepts:

```text
Dependency Inversion
    ↓
application depends on ports owned by the core

Dependency Injection
    ↓
Spring supplies the concrete adapter implementations
```

The composition root is therefore the deliberate place where both worlds are connected without pushing Spring into the business core.

---

# End-to-End Create Enrollment Validation

The complete POST flow has been executed successfully:

```text
POST /enrollments
      ↓
generated EnrollmentsApi
      ↓
generated EnrollmentRequest
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

# Read Side and Aggregate Rehydration

The read side is now implemented.

A newly created Enrollment follows the domain creation rule:

```text
new Enrollment(...)
        ↓
status = PENDING
```

But an Enrollment loaded from persistence may already be `PENDING`, `CONFIRMED`, or `CANCELLED`.

Using the normal constructor while reading would incorrectly reset every aggregate to `PENDING`. The domain therefore exposes:

```java
Enrollment.rehydrate(...)
```

to reconstruct an existing aggregate from persisted state.

Conceptually:

```text
CREATE
new aggregate
      ↓
apply initial domain rules
      ↓
PENDING

REHYDRATE
existing aggregate
      ↓
restore persisted state
      ↓
PENDING / CONFIRMED / CANCELLED
```

The read flow is now:

```text
GET /enrollments
      ↓
generated EnrollmentsApi
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
EnrollmentDocument
      ↓
Enrollment.rehydrate(...)
      ↓
Domain Enrollment
      ↓
EnrollmentResponse
```

A real end-to-end test was performed with MongoDB containing an Enrollment whose persisted status was `CONFIRMED`.

The GET response returned `status = CONFIRMED`, proving that the read path restores persisted state instead of applying the new-aggregate `PENDING` default.

---

# Interview-Level Summary

A concise DDD explanation:

> DDD models software around business concepts, rules, language, and boundaries. Entities are identity-based, Value Objects are value-based, Aggregates define consistency boundaries, and Aggregate Roots protect invariants.

A concise Hexagonal explanation:

> Hexagonal Architecture isolates the domain and application core from infrastructure. External actors enter through inbound ports, while the application accesses external systems through outbound ports. Adapters implement technical details such as REST or MongoDB, so dependency direction points toward the core.

---

# Current Learning Position

The DDD, Bounded Context, Hexagonal Architecture, Ports and Adapters, Clean Architecture, and API-first objectives for this architecture phase are now covered.

Completed concepts and implementation milestones include:

```text
DDD fundamentals
Entity identity
Value Objects
Aggregate Root
Domain invariants
Behavior-rich domain model
Framework-free domain tests
Bounded Contexts
Course / Enrollment context boundaries
Upstream / downstream relationship
Context Map fundamentals
Anti-Corruption Layer concept
Published Language
Conformist comparison
Bounded Context vs Aggregate vs Microservice vs database ownership
Application layer
Inbound use-case ports
Outbound ports
Dependency inversion
Framework-free application services
Mongo persistence adapter
Domain → MongoDB mapping
MongoDB → Domain mapping
Aggregate rehydration
Course REST adapter
Spring composition root
POST / GET end-to-end Hexagonal flows
Clean Architecture Dependency Rule
Domain/application dependency checks
OpenAPI 3.0 contract
Reusable request / response / error schemas
201 / 400 / 404 / 503 API behavior
Contract-driven validation
Maven OpenAPI validation
Invalid-spec build failure test
OpenAPI-generated request / response / error models
Generated Bean Validation constraints
Generated EnrollmentsApi interface
EnrollmentController implementing generated contract
Handwritten HTTP DTO removal
Generated error-model adoption
POST / GET end-to-end validation through generated API contract
```

Current architecture:

```text
                    OpenAPI
                       ↓
              generated EnrollmentsApi
                       ↓
               EnrollmentController
                 /             \
                ↓               ↓
CreateEnrollmentUseCase    FindEnrollmentsUseCase
                ↓               ↓
CreateEnrollmentService    FindEnrollmentsService
       /        \                 |
      ↓          ↓                ↓
CourseExists  SaveEnrollment   FindEnrollments
    Port          Port             Port
      ↑            ↑               ↑
CourseRestAdapter  └───── MongoEnrollmentAdapter
      ↓                          ↓
Course Service                 MongoDB
```


For milestone status, use [`ROADMAP.md`](ROADMAP.md).

---

[Back to README](../README.md)
