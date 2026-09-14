# Microservices

This document contains the learning material for the microservices evolution of the Climbing Management project.

Progress tracking remains centralized in `ROADMAP.md`.

---

# 1. Monolith vs Microservices

The original application was a single Spring Boot deployment unit:

```text
Client
  │
  ▼
climbing-management-sb
  │
  ├── Course logic
  └── Enrollment logic
```

Even if code is separated into packages, it is still a monolith while everything is built and deployed as one application.

A microservice introduces an independent application and deployment boundary.

```text
One application
      ↓
One process
      ↓
One deployment unit
```

becomes:

```text
Multiple applications
        ↓
Independent processes
        ↓
Independent deployment units
```

A microservice is therefore **not simply another Java package**.

---

# 2. First Service Boundary

The first extracted business capability is:

```text
Enrollment Management
```

The repository now contains two independent Spring Boot applications:

```text
climbing-management-sb/
│
├── pom.xml
├── src/
│
└── services/
    └── enrollment-service/
        ├── pom.xml
        └── src/
```

This is a **monorepo**:

```text
One Git repository
        ↓
Multiple independently runnable applications
```

Important distinction:

```text
Git repository boundary
        ≠
Application boundary
        ≠
Deployment boundary
        ≠
Database boundary
```

---

# 3. Independent Runtime

The applications run independently:

```text
Climbing Management
        ↓
localhost:8080

Enrollment Service
        ↓
localhost:8081
```

Each application has its own:

- `pom.xml`
- Spring Boot `main()` method
- dependencies
- configuration
- HTTP port
- JVM process

Therefore:

```text
Same Git repository
        ≠
Same application
```

Current runtime architecture:

```text
Windows
│
├── JVM #1
│     │
│     └── Climbing Management
│            localhost:8080
│
└── JVM #2
      │
      └── Enrollment Service
             localhost:8081
```

---

# 4. Enrollment API

The Enrollment Service exposes:

```text
POST /enrollments
GET  /enrollments
```

Create example:

```http
POST http://localhost:8081/enrollments
Content-Type: application/json
```

```json
{
  "courseId": 1,
  "studentName": "Angie"
}
```

Example successful response:

```json
{
  "id": "6aa658eb77f2c5799e02750d",
  "courseId": 1,
  "studentName": "Angie"
}
```

Response:

```text
201 Created
```

Retrieve enrollments:

```http
GET http://localhost:8081/enrollments
```

Example:

```json
[
  {
    "id": "6aa658eb77f2c5799e02750d",
    "courseId": 1,
    "studentName": "Angie"
  }
]
```

---

# 5. Evolution from In-Memory Storage

The first implementation deliberately used an in-memory list:

```text
HTTP
  │
  ▼
EnrollmentController
  │
  ▼
EnrollmentService
  │
  ▼
In-memory List
```

This was useful because it allowed the microservice boundary to be established before introducing persistence complexity.

The limitation was:

```text
Application restart
      ↓
In-memory data lost
```

The next step replaced the in-memory list with independent MongoDB persistence.

---

# 6. Independent Persistence

The Enrollment Service now owns its own MongoDB persistence.

```text
Enrollment Service
        │
        ▼
EnrollmentRepository
        │
        ▼
Spring Data MongoDB
        │
        ▼
enrollment_management
        │
        ▼
enrollments
```

Local MongoDB configuration:

```properties
spring.mongodb.uri=mongodb://localhost:27017/enrollment_management
```

The important concept is:

> Each service owns its data.

This does **not** necessarily require one physical database server per service.

For local development, multiple logical databases can exist on the same MongoDB server.

What matters is ownership.

```text
Course application
        │
        └── owns Course data

Enrollment Service
        │
        └── owns Enrollment data
```

Another service should not bypass the owning service and directly manipulate its database.

---

# 7. MongoDB Persistence Model

The Enrollment Service uses:

```text
EnrollmentDocument
├── id          String / ObjectId
├── courseId    Long
└── studentName String
```

Example MongoDB document:

```text
_id
ObjectId('6aa658eb77f2c5799e02750d')

courseId
Long('1')

studentName
"Angie"

_class
"com.rubenmarin.enrollmentservice.document.EnrollmentDocument"
```

The `_class` field is added by Spring Data MongoDB by default as type metadata.

---

# 8. Different Services Can Use Different ID Types

The Enrollment Service uses a MongoDB-generated identifier:

```text
Enrollment ID
      ↓
String / ObjectId
```

The Course application currently uses numeric identifiers:

```text
Course ID
      ↓
Long
```

Therefore:

```text
Enrollment
├── id          String
├── courseId    Long
└── studentName String
```

This is perfectly valid.

Different microservices do not need identical identifier strategies.

The Enrollment Service stores `courseId` as an external reference to data owned by the Course application.

---

# 9. Database Ownership

A poor service boundary would allow both services to directly access the same data model:

```text
Course Service ──────┐
                     ▼
                Shared Data
                     ▲
Enrollment Service ──┘
```

The preferred direction is:

```text
Course Service
      │
      ▼
Course Data


Enrollment Service
      │
      ▼
Enrollment Data
```

If Enrollment Service needs information about a course, it asks the owning service through its API.

It does not directly query the Course database.

---

# 10. Service-to-Service Communication

Before creating an enrollment, the Enrollment Service verifies that the requested course exists.

Architecture:

```text
POST /enrollments
        │
        ▼
EnrollmentController
        │
        ▼
EnrollmentService
        │
        ▼
CourseClient
        │
        │ HTTP
        ▼
Course Application
```

Local configuration:

```properties
course-service.base-url=http://localhost:8080
```

Enrollment Service:

```text
localhost:8081
```

Course application:

```text
localhost:8080
```

This is the first real network boundary between the two applications.

---

# 11. RestClient

The Enrollment Service uses Spring `RestClient` for synchronous HTTP communication.

Conceptually:

```text
Enrollment Service
        │
        │ HTTP GET
        ▼
Course Application
        │
        ├── 2xx
        │    ↓
        │ course exists
        │
        └── 404
             ↓
        course missing
```

In Spring Boot 4.1, the Enrollment Service includes the REST client infrastructure required to inject:

```java
RestClient.Builder
```

The client is built from:

```text
RestClient.Builder
        ↓
baseUrl
        ↓
RestClient
```

---

# 12. Monolith Call vs Microservice Call

Inside a monolith, this could be:

```text
EnrollmentService
        │
        ▼
CourseService
        │
        ▼
Normal Java method call
```

With separate services:

```text
Enrollment Service
        │
        ▼
Network
        │
        ▼
HTTP
        │
        ▼
Course Application
```

This distinction is fundamental.

A local Java call normally does not depend on:

- DNS
- Network availability
- TCP connections
- HTTP status codes
- Timeouts
- Remote service health

A microservice call does.

---

# 13. Successful Enrollment Flow

When the course exists:

```text
POST /enrollments
        │
        ▼
EnrollmentController
        │
        ▼
EnrollmentService
        │
        ▼
CourseClient
        │
        │ HTTP
        ▼
Course Application
        │
        ▼
2xx response
        │
        ▼
EnrollmentRepository
        │
        ▼
MongoDB
        │
        ▼
201 Created
```

The Enrollment Service only persists the enrollment after confirming that the Course exists.

---

# 14. Missing Course Flow

When the course does not exist:

```text
POST /enrollments
        │
        ▼
CourseClient
        │
        ▼
Course Application
        │
        ▼
404 Not Found
        │
        ▼
courseExists() = false
        │
        ▼
CourseNotFoundException
        │
        ▼
GlobalExceptionHandler
        │
        ▼
404 Not Found
```

Example:

```json
{
  "timestamp": "2026-09-13T08:50:30.565190300Z",
  "status": 404,
  "message": "Course not found: 2"
}
```

This is expected business behavior.

It should not result in:

```text
500 Internal Server Error
```

A `500` should normally represent an unexpected server-side failure.

---

# 15. Exception Translation

The remote Course application returns its HTTP result.

The Enrollment Service then translates that result into its own business meaning.

```text
Course Application
        │
        ▼
404
        │
        ▼
CourseClient
        │
        ▼
false
        │
        ▼
EnrollmentService
        │
        ▼
CourseNotFoundException
        │
        ▼
Enrollment API
        │
        ▼
404
```

This is an important microservices principle:

> Each service owns its API behavior even when the failure originated in another service.

---

# 16. Local Configuration Profiles

Environment-specific connectivity belongs in profile-specific configuration.

For local development:

```text
application-local.properties
```

contains values such as:

```properties
spring.mongodb.uri=mongodb://localhost:27017/enrollment_management

course-service.base-url=http://localhost:8080
```

This keeps local infrastructure configuration separate from environment-independent application settings.

Later environments may use different addresses:

```text
Local
   ↓
localhost

Docker
   ↓
Docker service names

Kubernetes
   ↓
Kubernetes Service DNS
```

---

# 17. MongoDB in Docker vs Local Applications

MongoDB currently runs inside Docker.

Applications running directly on Windows reach it through:

```text
localhost:27017
```

Applications running inside the Docker network would normally use:

```text
mongo:27017
```

Conceptually:

```text
Windows application
        │
        ▼
localhost:27017
        │
        ▼
Docker port mapping
        │
        ▼
MongoDB container
```

Inside Docker:

```text
Application container
        │
        ▼
mongo:27017
        │
        ▼
MongoDB container
```

This distinction will become important again when the Enrollment Service is containerized.

---

# 18. MongoDB Compass and Replica Set

MongoDB is configured as a replica set.

Inside Docker, the replica-set member may identify itself using a hostname such as:

```text
mongo:27017
```

That hostname works inside Docker networking but may not be resolvable from Windows.

For MongoDB Compass, a local direct connection can be used:

```text
mongodb://localhost:27017/enrollment_management?replicaSet=rs0&directConnection=true
```

`directConnection=true` tells the client to connect directly to the specified MongoDB instance instead of relying on replica-set member discovery.

---

# 19. Testing Configuration

The local application configuration should not automatically be assumed by tests.

For example:

```text
application-local.properties
```

is only loaded when the `local` profile is active.

Tests that require configuration such as:

```properties
course-service.base-url=http://localhost:8080
```

can provide that property directly through the test configuration.

Example:

```java
@SpringBootTest(properties = {
        "course-service.base-url=http://localhost:8080"
})
class EnrollmentServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

This allows the Spring context to create `CourseClient` during tests without requiring the local runtime profile.

---

# 20. Current Architecture

The project now looks like:

```text
                         Client
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
   Climbing Management API      Enrollment Service
       Spring Boot                  Spring Boot
          :8080                       :8081
              │                         │
              │                         ▼
              │                EnrollmentRepository
              │                         │
              │                         ▼
              │                     MongoDB
              │                enrollment_management
              │
              ◄──────── HTTP ──────────┘
                   course validation
```

The Enrollment Service now has:

- Independent runtime
- Independent Maven build
- Independent API
- Independent persistence
- Its own MongoDB database ownership
- Service-to-service REST communication
- Remote 404 translation
- Separate local configuration

---

# 21. Important Distributed-System Lesson

A normal local Java method call can fail because of application logic.

A remote service call introduces additional failure modes:

```text
Course not found
Connection refused
Network unavailable
Timeout
DNS failure
Remote service returns 500
Malformed response
Slow response
Remote service restarts
```

Therefore:

```text
Remote call
    ≠
Local method call
```

Once a system becomes distributed, the network becomes part of the architecture.

---

# 22. Dependency Failure Handling

The Enrollment Service depends on the Course application to validate that a course exists before creating an enrollment.

Because this validation happens through HTTP, the dependency can be unavailable even when the Enrollment Service itself is healthy.

```text
Client
  │
  ▼
Enrollment Service :8081
  │
  ▼
CourseClient
  │
  │ HTTP
  ▼
Course Application :8080
  X
DOWN
```

This is fundamentally different from a normal Java method call inside a monolith.

---

# 23. Observed Failure

The Course application was intentionally stopped while the Enrollment Service remained running.

A request was sent to:

```http
POST http://localhost:8081/enrollments
Content-Type: application/json
```

```json
{
  "courseId": 1,
  "studentName": "Failure Test"
}
```

Initially the dependency failure propagated as an unexpected server error.

The underlying problem was a failed network connection to the Course application.

Conceptually:

```text
Enrollment Service
        │
        ▼
CourseClient
        │
        ▼
HTTP connection
        │
        X
Connection cannot be established
```

The Enrollment Service itself was alive.

The failure happened because one of its dependencies was unavailable.

---

# 24. Low-Level Network Failure

Low-level Java networking exceptions should not leak directly into the service API.

Spring's REST client infrastructure translates connection and I/O problems into:

```text
ResourceAccessException
```

The important abstraction is:

```text
Low-level networking failure
        │
        ▼
ResourceAccessException
        │
        ▼
Application-specific exception
```

The Enrollment Service therefore introduces:

```text
CourseServiceUnavailableException
```

Example:

```java
public class CourseServiceUnavailableException extends RuntimeException {

    public CourseServiceUnavailableException(Throwable cause) {
        super("Course Service is currently unavailable", cause);
    }
}
```

This avoids coupling the rest of the application to low-level networking exceptions.

---

# 25. Translating Infrastructure Failures

`CourseClient` catches the infrastructure exception and translates it into an application-specific exception.

Conceptually:

```java
try {

        return restClient.get()
            .uri(...)
            .exchange((request, response) -> {

        if (response.getStatusCode().is2xxSuccessful()) {
        return true;
        }

        if (response.getStatusCode().value() == 404) {
        return false;
        }

        throw new IllegalStateException(
                        "Unexpected response from Course Service: "
                                + response.getStatusCode()
                );
                        });

                        } catch (ResourceAccessException exception) {

        throw new CourseServiceUnavailableException(exception);
}
```

The important architectural boundary is:

```text
HTTP / network layer
        │
        ▼
ResourceAccessException
        │
        ▼
CourseClient
        │
        ▼
CourseServiceUnavailableException
```

The rest of the application does not need to understand socket-level failures.

---

# 26. 404 vs 503

A missing Course and an unavailable Course Service are two completely different situations.

## Course does not exist

```text
Enrollment Service
        │
        ▼
Course Service
        │
        ▼
HTTP 404
        │
        ▼
CourseNotFoundException
        │
        ▼
404 Not Found
```

The remote service answered successfully.

The requested resource simply does not exist.

## Course Service is unavailable

```text
Enrollment Service
        │
        ▼
CourseClient
        │
        X
Cannot connect
        │
        ▼
CourseServiceUnavailableException
        │
        ▼
503 Service Unavailable
```

The resource may exist.

The Enrollment Service simply cannot verify it because the dependency is currently unavailable.

Therefore:

```text
Course absent
    → 404 Not Found

Course Service unavailable
    → 503 Service Unavailable
```

---

# 27. Global Exception Mapping

The Enrollment Service maps the application-specific exception using the global exception handler.

Example:

```java
@ExceptionHandler(CourseServiceUnavailableException.class)
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public Map<String, Object> handleCourseServiceUnavailable(
        CourseServiceUnavailableException exception) {

    return Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
            "message", exception.getMessage()
    );
}
```

The API response is:

```json
{
  "timestamp": "2026-09-14T07:05:49.182284500Z",
  "message": "Course Service is currently unavailable",
  "status": 503
}
```

This exposes a stable API contract instead of implementation-specific networking details.

---

# 28. Current Failure Matrix

The Enrollment API now distinguishes between four important outcomes:

| Situation | Result |
| --- | --- |
| Course exists | `201 Created` |
| Course does not exist | `404 Not Found` |
| Course Service unavailable | `503 Service Unavailable` |
| Course Service too slow | `503 Service Unavailable` |

Conceptually:

```text
POST /enrollments
        │
        ▼
CourseClient
        │
        ├── 2xx
        │     ↓
        │ course exists
        │     ↓
        │ save
        │     ↓
        │ 201
        │
        ├── 404
        │     ↓
        │ CourseNotFoundException
        │     ↓
        │ 404
        │
        ├── connection failure
        │     ↓
        │ CourseServiceUnavailableException
        │     ↓
        │ 503
        │
        └── timeout
              ↓
        ResourceAccessException
              ↓
        CourseServiceUnavailableException
              ↓
             503
```

---

# 29. Failure Isolation

The Enrollment Service validates the Course before persisting the enrollment.

```text
POST /enrollments
        │
        ▼
validate course
        │
        ├── SUCCESS
        │      ↓
        │ EnrollmentRepository.save()
        │      ↓
        │ MongoDB
        │
        └── FAILURE
               ↓
           no save()
               ↓
          MongoDB unchanged
```

This ordering is important.

A dependency failure must not leave partially written local state.

---

# 30. Failure Isolation Test

While the Course application was stopped, an enrollment creation was attempted.

The request returned:

```text
503 Service Unavailable
```

The stored enrollments were then checked with:

```http
GET http://localhost:8081/enrollments
```

The database still contained only previously successful enrollments.

Example:

```json
[
  {
    "id": "6aa658eb77f2c5799e02750d",
    "courseId": 1,
    "studentName": "Angie"
  },
  {
    "id": "6aa65fd7f963987ac79d377b",
    "courseId": 1,
    "studentName": "ruben"
  }
]
```

The failed enrollment was not stored.

This confirms:

```text
Remote validation failed
        ↓
repository.save() not executed
        ↓
MongoDB unchanged
```

---

# 31. Recovery Test

The Course application was then started again.

No restart of the Enrollment Service was required.

A new request was sent:

```json
{
  "courseId": 1,
  "studentName": "Recovery Test"
}
```

The enrollment was successfully persisted.

This proves:

```text
Dependency DOWN
      ↓
503
      ↓
no data corruption

Dependency UP again
      ↓
normal communication resumes
      ↓
201 Created
```

The Enrollment Service does not remain permanently broken after a temporary dependency outage.

---

# 32. Failure Isolation Principle

An important distributed-system principle demonstrated by this milestone is:

> A failure in one service should not corrupt the state of another service.

In this implementation:

```text
Course Service failure
        │
        ▼
Enrollment creation fails
        │
        ▼
Enrollment MongoDB remains consistent
```

This is a simple example of failure isolation.

More complex distributed systems may require patterns such as:

```text
Timeout
Retry
Circuit Breaker
Saga
Outbox Pattern
Idempotency
```

depending on the operation.

---

# 33. Local Call vs Remote Call

Inside a monolith:

```text
EnrollmentService
        │
        ▼
CourseService
        │
        ▼
Java method
```

The call is extremely fast and exists inside one JVM.

With microservices:

```text
Enrollment Service
        │
        ▼
HTTP Client
        │
        ▼
Network
        │
        ▼
Course Service
```

The call can fail because of:

```text
Service down
Network failure
DNS failure
Connection refused
Timeout
Slow response
HTTP 5xx
Invalid response
Deployment/restart
```

This is why distributed applications need resilience mechanisms.

---

# 34. Current Architecture After Failure Handling

```text
                         Client
                           │
                           ▼
                  Enrollment Service
                       :8081
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
         CourseClient          EnrollmentRepository
              │                         │
              │ HTTP                    ▼
              ▼                     MongoDB
     Course Application          enrollment_management
          :8080
              │
      ┌───────┴────────┐
      │                │
     2xx              404
      │                │
      ▼                ▼
   continue     CourseNotFoundException

connection failure / timeout
      │
      ▼
ResourceAccessException
      │
      ▼
CourseServiceUnavailableException
      │
      ▼
503 Service Unavailable
```

---

# 35. What We Have Learned So Far

The microservices evolution now demonstrates:

- Monolith vs microservices
- Service boundaries
- Independent Spring Boot applications
- Monorepo structure
- Independent runtime
- Independent Maven builds
- Independent APIs
- Database-per-service ownership
- MongoDB persistence
- Different ID strategies between services
- Service-to-service REST communication
- Spring `RestClient`
- Remote resource validation
- HTTP status translation
- Business failure vs infrastructure failure
- `404 Not Found`
- `503 Service Unavailable`
- Failure isolation
- Recovery after dependency restoration
- Connect timeout configuration
- Read timeout configuration
- Slow dependency simulation
- Timeout failure handling
- No persistence after timeout
- Spring resilience method support
- Selective retries
- Bounded retry attempts
- Retry recovery after transient failure
- No retry for business `404`
- Retry exhaustion
- Failure isolation after retry exhaustion
- Retry load amplification
- Idempotency considerations

---

# 36. Timeout Handling

A remote service can be running and reachable while still responding too slowly.

This is dangerous because the calling service may continue waiting for the dependency.

Without a bounded timeout:

```text
Enrollment request
        │
        ▼
Course Service slow
        │
        ▼
request thread waits
        │
        ▼
more requests arrive
        │
        ▼
more threads wait
        │
        ▼
resource exhaustion
```

Therefore remote calls should have explicit time limits.

---

## Connect Timeout

The **connect timeout** defines how long the client is willing to wait while establishing a network connection.

```text
Enrollment Service
        │
        ▼
attempt connection
        │
        X
connection cannot be established
        │
        ▼
connect timeout / connection failure
```

The Enrollment Service currently uses:

```text
connect timeout = 2 seconds
```

---

## Read Timeout

The **read timeout** defines how long the client waits for the remote service to return data after the connection has already been established.

```text
Enrollment Service
        │
        ▼
connection established
        │
        ▼
Course Service processing...
        │
        │ too slow
        ▼
read timeout
```

The Enrollment Service currently uses:

```text
read timeout = 2 seconds
```

---

# 37. RestClient Timeout Configuration

The `CourseClient` uses Java's HTTP client together with Spring's `JdkClientHttpRequestFactory`.

```java
HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(httpClient);

requestFactory.setReadTimeout(Duration.ofSeconds(2));

this.restClient = builder
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
```

The resulting architecture is:

```text
CourseClient
    │
    ├── connect timeout = 2s
    │
    └── read timeout = 2s
    │
    ▼
RestClient
    │
    ▼
Course Service
```

An important implementation detail is that creating and configuring the request factory is not enough.

It must actually be attached to the `RestClient`:

```text
Create requestFactory
        ↓
Configure timeouts
        ↓
Attach requestFactory to RestClient
        ↓
RestClient uses configured timeouts
```

Without:

```java
.requestFactory(requestFactory)
```

the custom timeout configuration would not be used.

---

# 38. Timeout Test

To test the read timeout, a temporary artificial delay was introduced into the Course application's endpoint.

The Course endpoint temporarily executed:

```java
try {
    Thread.sleep(5000);
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
}
```

This simulated:

```text
Course Service response time = 5 seconds
```

while the Enrollment Service was configured with:

```text
read timeout = 2 seconds
```

The test therefore became:

```text
POST /enrollments
        │
        ▼
Enrollment Service
        │
        ▼
CourseClient
        │
        ▼
HTTP connection succeeds
        │
        ▼
Course Service
        │
        ▼
Thread.sleep(5000)
        │
        ▼
Enrollment waits 2 seconds
        │
        X
read timeout
```

The Enrollment Service returned:

```json
{
  "status": 503,
  "message": "Course Service is currently unavailable"
}
```

---

# 39. Timeout Exception Flow

The timeout is exposed by the HTTP client as a resource-access failure.

```text
Course Service slow
        │
        ▼
read timeout
        │
        ▼
ResourceAccessException
        │
        ▼
CourseServiceUnavailableException
        │
        ▼
GlobalExceptionHandler
        │
        ▼
503 Service Unavailable
```

This means the public API does not need to expose the technical distinction between:

```text
Course Service DOWN
```

and:

```text
Course Service too SLOW
```

Both mean that the Enrollment Service cannot currently complete the operation.

Therefore:

```text
Dependency unavailable
        ↓
503

Dependency too slow
        ↓
503
```

The internal technical cause remains available through the exception chain and logs.

---

# 40. Timeout and Remote Server Execution

A client timeout does not necessarily stop the code already running inside the remote server.

During the test:

```text
Enrollment Service
        │
        │ waits 2 seconds
        X
timeout
        │
        ▼
returns 503
```

while the Course application could still continue:

```text
Course Service
        │
        ▼
Thread.sleep(5000)
        │
        ▼
sleep finishes
        │
        ▼
controller execution continues
```

Therefore:

> Client timeout means the caller stops waiting. It does not necessarily cancel the remote server-side operation.

This distinction becomes especially important when remote operations modify data.

---

# 41. Timeout and Data Consistency

The timeout occurred before the Enrollment Service persisted the enrollment.

The flow was:

```text
POST /enrollments
        │
        ▼
validate Course remotely
        │
        ▼
timeout
        │
        ▼
CourseServiceUnavailableException
        │
        ▼
EnrollmentService.create() exits
        │
        ▼
EnrollmentRepository.save() NOT executed
```

MongoDB was checked afterward.

The timed-out enrollment was not present.

Therefore:

```text
Remote validation timeout
        ↓
local write aborted
        ↓
MongoDB unchanged
```

This confirms the same failure-isolation behavior previously tested when the Course Service was completely unavailable.

---

# 42. Timeout Recovery Test

After the timeout test, the artificial:

```java
Thread.sleep(5000);
```

was removed from the Course application.

The normal enrollment request was executed again.

```text
Course responds in < 2 seconds
        ↓
no timeout
        ↓
validation succeeds
        ↓
EnrollmentRepository.save()
        ↓
MongoDB
        ↓
201 Created
```

This confirms that the timeout configuration does not interfere with healthy requests.

---

# 43. Current Resilience Behavior

The Enrollment Service currently behaves as follows:

```text
Course exists and responds quickly
        ↓
201 Created
```

```text
Course does not exist
        ↓
404 Not Found
        ↓
no retry
```

```text
Transient dependency failure
        ↓
CourseServiceUnavailableException
        ↓
retry
        ↓
dependency recovers
        ↓
201 Created
```

```text
Persistent dependency failure
        ↓
initial attempt
        ↓
retry #1
        ↓
retry #2
        ↓
503 Service Unavailable
```

In failure situations where course validation never succeeds:

```text
validation failure
        ↓
EnrollmentRepository.save() not executed
        ↓
Enrollment not persisted
```

---

# 44. Why Timeouts Matter

Without timeouts:

```text
slow dependency
      ↓
waiting request threads
      ↓
more incoming requests
      ↓
more blocked resources
      ↓
calling service becomes slow
      ↓
possible cascading failure
```

With bounded timeouts:

```text
slow dependency
      ↓
wait maximum configured time
      ↓
fail fast
      ↓
release resources
      ↓
protect calling service
```

The key principle is:

> A remote call must have a bounded waiting time.

---

# 45. Retry Handling

A retry allows a temporary failure to be attempted again.

The Enrollment Service retries the Course Service validation when the failure is considered transient.

Spring resilience support is enabled in the Enrollment Service with:

```java
@EnableResilientMethods
```

The retry policy is configured on:

```java
CourseClient.courseExists()
```

using:

```java
@Retryable(
        includes = CourseServiceUnavailableException.class,
        maxRetries = 2,
        delay = 500
)
```

The configured policy is:

```text
maxRetries = 2
delay      = 500 ms
```

`maxRetries = 2` means two retries **after** the initial call.

```text
Attempt #1
    │
    X CourseServiceUnavailableException
    │
    │ wait 500 ms
    ▼
Retry #1
    │
    X CourseServiceUnavailableException
    │
    │ wait 500 ms
    ▼
Retry #2
    │
    ├── success → continue
    │
    └── failure → propagate exception
```

Therefore:

```text
1 initial attempt
+
2 retries
=
3 maximum remote calls
```

The retry loop is bounded.

It does not continue indefinitely.

---

# 46. Selective Retry

The Enrollment Service does not retry every possible failure.

The retry policy includes:

```java
CourseServiceUnavailableException.class
```

The application translates network and I/O failures first:

```text
connection failure / read timeout
        ↓
ResourceAccessException
        ↓
CourseServiceUnavailableException
        ↓
retry
```

This allows the retry policy to work with an application-specific exception rather than low-level networking details.

A normal business failure such as:

```text
404 Course Not Found
```

is not retried.

The flow is:

```text
Course Service
      ↓
404
      ↓
courseExists() returns false
      ↓
CourseNotFoundException
      ↓
404 returned to client
```

This distinction is fundamental:

```text
Transient infrastructure failure
        ↓
retry may help

Business result
        ↓
retry usually does not help
```

The implementation is deliberately selective instead of retrying generic `Exception`.

---

# 47. Retry Recovery Test

To prove that retry can recover from a transient dependency failure, the Course application was temporarily modified so that only the first validation request was slow.

A counter was used only for the experiment.

Conceptually:

```text
Course validation request #1
        ↓
sleep 5 seconds
        ↓
Enrollment read timeout after 2 seconds
```

The Enrollment Service translated the timeout:

```text
read timeout
        ↓
ResourceAccessException
        ↓
CourseServiceUnavailableException
        ↓
@Retryable
```

After the configured delay:

```text
500 ms
```

the remote call was attempted again.

The second Course request responded normally:

```text
POST /enrollments
        ↓
Course request #1
        ↓
slow response
        ↓
read timeout
        ↓
CourseServiceUnavailableException
        ↓
wait 500 ms
        ↓
Course request #2
        ↓
200 OK
        ↓
course validation succeeds
        ↓
EnrollmentRepository.save()
        ↓
MongoDB
        ↓
201 Created
```

The successful response confirmed that the retry recovered from the transient failure.

This demonstrates:

> A bounded retry can allow a request to recover automatically when the dependency failure is temporary.

---

# 48. Retry Does Not Cancel Previous Remote Work

During the retry recovery experiment, the Course Service eventually executed two database queries.

This is expected.

The first remote request did not automatically stop executing when the Enrollment Service timed out.

Conceptually:

```text
Enrollment Service                  Course Service

request #1 ───────────────────────→ sleep 5s
       │                              │
       X timeout after 2s             │
       │                              │
       ▼                              │
wait 500 ms                           │
       │                              │
       ▼                              │
retry #1 ──────────────────────────→ request #2
                                      │
                                      ▼
                                   SELECT
                                      │
                                      ▼
                                    200 OK

                                      meanwhile...

request #1 finishes sleeping
        │
        ▼
      SELECT
```

Therefore a retry can temporarily create multiple executions of the same remote operation.

This is one of the reasons retries must be designed carefully.

---

# 49. 404 No-Retry Test

A request was made using a Course identifier that did not exist.

The Course Service received exactly one validation request:

```text
Course validation request #1
        ↓
database lookup
        ↓
404
```

The Enrollment Service returned:

```json
{
  "message": "Course not found: 999999",
  "status": 404
}
```

There was no:

```text
Course validation request #2
```

and no:

```text
Course validation request #3
```

This proved that the retry configuration is selective.

```text
404
 ↓
business outcome
 ↓
no retry
```

A retry would not normally help when the Course Service has successfully answered that the resource does not exist.

---

# 50. Retry Exhaustion Test

Another experiment deliberately made every Course validation request slower than the configured read timeout.

The Enrollment Service therefore executed:

```text
Attempt #1
    ↓
timeout
    ↓
Retry #1
    ↓
timeout
    ↓
Retry #2
    ↓
timeout
    ↓
stop retrying
```

The Course Service received exactly three validation requests:

```text
Course validation request #1
Course validation request #2
Course validation request #3
```

The final Enrollment API response was:

```json
{
  "message": "Course Service is currently unavailable",
  "status": 503
}
```

This proves that retries are bounded.

```text
maxRetries = 2
        ↓
3 total attempts maximum
        ↓
failure propagated
        ↓
503
```

The failed enrollment was checked afterward.

It was not persisted.

Therefore:

```text
all retry attempts fail
        ↓
CourseServiceUnavailableException
        ↓
EnrollmentRepository.save() not executed
        ↓
MongoDB unchanged
```

Failure isolation remains intact even when retry is enabled.

---

# 51. Retry Trade-Offs

Retries improve resilience when failures are temporary.

However, they also increase load.

Without retry:

```text
1 incoming request
        ↓
1 remote call
```

With two retries:

```text
1 incoming request
        ↓
up to 3 remote calls
```

If the dependency is already overloaded:

```text
Dependency slow
      ↓
requests fail
      ↓
clients retry
      ↓
more requests reach dependency
      ↓
dependency becomes even slower
      ↓
more retries
```

This can create a:

```text
Retry storm
```

Retries therefore need:

```text
bounded attempts
        +
delay / backoff
        +
timeouts
        +
careful exception selection
```

A retry is not automatically beneficial merely because a request failed.

The failure type and the operation being retried matter.

---

# 52. Why Idempotency Matters

The current remote operation is:

```http
GET /jpa/courses/{id}
```

A GET request is expected to be idempotent.

Executing the same Course lookup multiple times should not modify Course state.

Therefore retrying this operation is comparatively safe.

State-changing operations require more care.

For example:

```text
POST payment
      ↓
server processes payment
      ↓
response is lost / client times out
      ↓
client retries POST
      ↓
possible duplicate payment
```

From the caller's perspective, a timeout can make the outcome ambiguous.

The caller may not know whether the remote side completed the operation.

For state-changing operations, distributed systems may therefore require mechanisms such as:

```text
Idempotency keys
Deduplication
Unique constraints
Transactional outbox
```

depending on the use case.

The important principle is:

> Retry safety depends not only on the failure but also on the semantics of the operation being retried.

---

# 53. Next Resilience Topic — Circuit Breaker

Timeouts and retries solve different problems:

```text
Timeout
   ↓
do not wait indefinitely

Retry
   ↓
try again after a transient failure
```

But retries have a limitation.

If the dependency is continuously unhealthy:

```text
Request
   ↓
failure
   ↓
retry
   ↓
failure
   ↓
retry
   ↓
failure
```

every incoming Enrollment request can still create multiple failing Course calls.

The next resilience mechanism is therefore:

```text
Circuit Breaker
```

A Circuit Breaker stops repeatedly calling a dependency that is already known to be failing.

---

# 54. Circuit Breaker

A Circuit Breaker commonly has three states:

```text
CLOSED
  │
  │ failures exceed threshold
  ▼
OPEN
  │
  │ wait period
  ▼
HALF-OPEN
  │
  ├── success → CLOSED
  │
  └── failure → OPEN
```

## CLOSED

Normal state.

```text
Enrollment Service
        │
        ▼
Course Service
```

Calls are allowed.

Failures are monitored.

---

## OPEN

Too many failures have occurred.

```text
Enrollment Service
        │
        X
Course Service
```

Calls are rejected without sending another network request to the failing dependency.

This allows the caller to fail quickly and protects the dependency from repeated calls.

It helps prevent:

```text
Repeated network calls
        ↓
Timeout waits
        ↓
Retry amplification
        ↓
Resource exhaustion
        ↓
Cascading failure
```

---

## HALF-OPEN

After a configured period, the Circuit Breaker allows a limited number of test requests.

```text
Circuit OPEN
      ↓
wait
      ↓
HALF-OPEN
      ↓
test request
```

If the dependency has recovered:

```text
HALF-OPEN
    ↓
success
    ↓
CLOSED
```

If the dependency is still failing:

```text
HALF-OPEN
    ↓
failure
    ↓
OPEN
```

The next implementation milestone will add this behavior around the Course Service dependency.

---

# 55. Resilience Goal

The objective of resilience is not to pretend failures do not happen.

The goal is to:

```text
Detect failure
      ↓
Contain failure
      ↓
Fail predictably
      ↓
Protect resources
      ↓
Recover automatically when possible
```

Microservices should be designed assuming:

> Remote dependencies will eventually fail.

The architecture must therefore define what happens when they do.

The resilience mechanisms introduced so far are:

```text
Timeouts ✅
   ↓
Retries ✅
   ↓
Circuit Breaker ← NEXT
```

---

# 56. Current Microservices Learning Position

Current progress:

```text
Monolith
   ↓
Identify service boundary
   ↓
Extract Enrollment Service
   ↓
Independent runtime
   ↓
Independent persistence
   ↓
Database ownership
   ↓
REST communication
   ↓
Remote validation
   ↓
Business error translation
   ↓
Dependency failure handling
   ↓
Failure isolation
   ↓
Recovery
   ↓
Timeout configuration
   ↓
Slow dependency test
   ↓
Timeout handling
   ↓
Selective retry
   ↓
Transient failure recovery
   ↓
404 no-retry verification
   ↓
Retry exhaustion verification
   ↓
Failure isolation with retries
   ↓
CURRENT POSITION
   ↓
Circuit Breaker
   ↓
Independent containerization
   ↓
Independent Kubernetes deployment
```

---

[Back to README](../README.md)