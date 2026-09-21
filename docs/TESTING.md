# Testing — Enrollment Service

Learning notes for unit testing, Mockito, Spring test slices, integration testing and Testcontainers in the Climbing Management project.

[Back to README](../README.md) · [Progress and next steps](ROADMAP.md)

Progress checkboxes belong only in `ROADMAP.md`. This document records the testing theory, examples and checkpoints completed so far.

---

## 1. Testing strategy

The project uses different test types for different boundaries.

```text
                       REST / API tests
                             ⏳
                              │
                       Integration tests
                              │
                    Spring + real infrastructure
                              │
                       Testcontainers
                              │
                 Persistence / adapter integration
                              │
                     Unit tests + Mockito
                              │
                  Pure domain unit tests
```

The goal is to use the lightest useful test for each responsibility:

- domain invariants → pure unit tests
- application orchestration → fake ports or Mockito
- persistence mapping → real database integration tests
- REST/security behavior → API integration tests later in the phase

---

## 2. Existing pure unit tests

Before the dedicated Testing phase began, the DDD/Hexagonal work had already introduced pure unit tests.

Current Enrollment Service tests include:

```text
EnrollmentServiceApplicationTests
CourseRestAdapterTest
MongoEnrollmentAdapterTest
CreateEnrollmentServiceTest
FindEnrollmentsServiceTest
EnrollmentTest
```

The application-service tests use hand-written fake ports.

Example:

```java
CourseExistsPort courseExistsPort = courseId -> true;

SaveEnrollmentPort saveEnrollmentPort =
        enrollment -> enrollment;
```

This style is useful when ports are small functional interfaces.

```text
Advantages
├── very explicit
├── no mocking framework required
├── fast
└── framework-free
```

The missing-Course test also proved that persistence must not happen after validation fails.

---

## 3. Why keep fake tests and add Mockito tests?

The existing tests were not replaced.

Instead, separate Mockito classes were added so both styles can be compared:

```text
CreateEnrollmentServiceTest
→ JUnit + hand-written fake ports

CreateEnrollmentServiceMockitoTest
→ JUnit + Mockito mocks
```

Both are **unit tests**.

Mockito does not make a test an integration test.

---

## 4. Mockito setup

Mockito is integrated with JUnit 5 through:

```java
@ExtendWith(MockitoExtension.class)
```

Collaborators are mocked:

```java
@Mock
private CourseExistsPort courseExistsPort;

@Mock
private SaveEnrollmentPort saveEnrollmentPort;
```

The real class under test is created with those mocks injected:

```java
@InjectMocks
private CreateEnrollmentService createEnrollmentService;
```

Conceptually:

```text
@Mock
→ test doubles for outbound dependencies

@InjectMocks
→ real application service
  with mocks injected through the constructor
```

---

## 5. Stubbing with `when(...).thenReturn(...)`

Stubbing defines collaborator behavior.

```java
when(courseExistsPort.existsById(any()))
        .thenReturn(true);
```

Meaning:

```text
when CourseExistsPort is called
        ↓
pretend the Course exists
```

The important rule is:

```text
mock dependencies
NOT the class under test
```

So this is correct:

```java
when(findEnrollmentsPort.findAll())
        .thenReturn(enrollments);
```

while stubbing `findEnrollmentsService.findAll()` would defeat the purpose of the unit test.

---

## 6. Returning the received argument with `thenAnswer(...)`

The persistence port can behave like the old hand-written fake:

```java
when(saveEnrollmentPort.save(any(Enrollment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
```

This means:

```text
save(enrollment)
        ↓
return that same enrollment
```

It is the Mockito equivalent of:

```java
enrollment -> enrollment
```

---

## 7. State assertions vs interaction verification

JUnit assertions answer:

```text
What result/state did I get?
```

Example:

```java
assertEquals(
        EnrollmentStatus.PENDING,
        created.getStatus()
);
```

Mockito verification answers:

```text
Which collaborators did the class under test call?
```

Example:

```java
verify(courseExistsPort)
        .existsById(any());
```

and:

```java
verify(saveEnrollmentPort)
        .save(any(Enrollment.class));
```

Both are useful because they verify different things.

---

## 8. Verifying that persistence never happens

The failure path is an important orchestration rule:

```text
Course does not exist
        ↓
CourseNotFoundException
        ↓
Enrollment must NOT be saved
```

Mockito expresses this directly:

```java
verify(saveEnrollmentPort, never())
        .save(any(Enrollment.class));
```

This is a clear interaction-level assertion.

---

## 9. `ArgumentCaptor`

A normal verify proves that `save(...)` was called:

```java
verify(saveEnrollmentPort)
        .save(any(Enrollment.class));
```

`ArgumentCaptor` lets the test inspect the exact object sent to persistence:

```java
ArgumentCaptor<Enrollment> enrollmentCaptor =
        ArgumentCaptor.forClass(Enrollment.class);

verify(saveEnrollmentPort)
        .save(enrollmentCaptor.capture());

Enrollment savedEnrollment =
        enrollmentCaptor.getValue();
```

The captured object can then be checked:

```java
assertEquals(
        new CourseId(10L),
        savedEnrollment.getCourseId()
);

assertEquals(
        new StudentName("Rubén"),
        savedEnrollment.getStudentName()
);

assertEquals(
        EnrollmentStatus.PENDING,
        savedEnrollment.getStatus()
);
```

This verifies what the application service actually tried to persist.

---

## 10. `CreateEnrollmentServiceMockitoTest`

The Mockito version covers both main write-side paths.

### Course exists

```text
CourseExistsPort → true
        ↓
CreateEnrollmentService
        ↓
new Enrollment
        ↓
SaveEnrollmentPort.save(...)
        ↓
result returned
```

Verified behavior includes:

```text
Course validation performed
Enrollment created
PENDING status
persistence called
saved object inspectable through ArgumentCaptor
```

### Course does not exist

```text
CourseExistsPort → false
        ↓
CourseNotFoundException
        ↓
SaveEnrollmentPort never called
```

This remains a pure unit test:

```text
no Spring
no Docker
no MongoDB
no Keycloak
no Course Service
```

---

## 11. `FindEnrollmentsServiceMockitoTest`

The read-side service is also tested with Mockito.

### Existing enrollments

```java
when(findEnrollmentsPort.findAll())
        .thenReturn(enrollments);
```

The real service is called:

```java
List<Enrollment> result =
        findEnrollmentsService.findAll();
```

The test verifies that the data is returned and the outbound port is invoked.

### No enrollments

```java
when(findEnrollmentsPort.findAll())
        .thenReturn(List.of());
```

The test checks:

```java
assertTrue(enrollmentList.isEmpty());
```

and:

```java
verify(findEnrollmentsPort)
        .findAll();
```

---

## 12. Why integration tests are needed

Mocks and fakes cannot prove that real infrastructure works.

A mocked repository cannot prove:

```text
Spring Data mapping works
MongoDB driver works
Mongo documents are stored correctly
repository operations work against MongoDB
adapter ↔ persistence mapping works with actual data
```

That requires a real database integration test.

The persistence boundary therefore becomes:

```text
MongoEnrollmentAdapter
        ↓
Spring Data MongoDB
        ↓
real MongoDB
```

---

## 13. Testcontainers dependencies

The Enrollment Service added test support for:

```text
spring-boot-testcontainers
Testcontainers JUnit Jupiter
Testcontainers MongoDB
Spring Boot MongoDB test slice
```

The exact goal is:

```text
JUnit starts test
        ↓
Testcontainers starts MongoDB in Docker
        ↓
Spring Boot receives the connection automatically
        ↓
test runs against real MongoDB
        ↓
container lifecycle managed by the test framework
```

This means the integration test does not depend on the developer's manually installed local MongoDB instance.

---

## 14. Testcontainers annotations

### `@Testcontainers`

```java
@Testcontainers
```

Integrates Testcontainers with JUnit 5.

### `@Container`

```java
@Container
static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:7.0");
```

Marks the MongoDB container as part of the test lifecycle.

### `@ServiceConnection`

```java
@ServiceConnection
```

Lets Spring Boot obtain the MongoDB connection details from the Testcontainer automatically.

No fixed local MongoDB port is required.

---

## 15. Why `@DataMongoTest` instead of `@SpringBootTest`

The first Testcontainers attempt used:

```java
@SpringBootTest
```

That loaded the whole Enrollment Service context, including unrelated infrastructure.

The test then failed because `CourseRestAdapter` required:

```text
course-service.base-url
```

But the test only wanted to exercise Mongo persistence.

The correct solution was not to invent a fake property for an unrelated adapter. The test boundary was narrowed to:

```java
@DataMongoTest
```

and the adapter under test was imported explicitly:

```java
@Import(MongoEnrollmentAdapter.class)
```

So the test loads roughly:

```text
MongoDB infrastructure
Spring Data MongoDB
Mongo repositories
MongoEnrollmentAdapter
```

instead of the complete application.

This is faster, clearer and more focused.

---

## 16. `MongoEnrollmentAdapterIntegrationTest`

The integration test is defined around the Mongo persistence slice:

```java
@DataMongoTest
@Testcontainers
@Import(MongoEnrollmentAdapter.class)
class MongoEnrollmentAdapterIntegrationTest {
```

The real MongoDB container is wired with:

```java
@Container
@ServiceConnection
static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:7.0");
```

The real adapter and repository are injected:

```java
@Autowired
private MongoEnrollmentAdapter mongoEnrollmentAdapter;

@Autowired
private EnrollmentRepository enrollmentRepository;
```

No repository mock is used in this integration test.

---

## 17. Database cleanup and test isolation

Each test starts from an empty repository:

```java
@BeforeEach
void cleanDatabase() {
    enrollmentRepository.deleteAll();
}
```

This prevents one test from depending on data created by another.

```text
Test A data
    ✗ must not leak into
Test B
```

---

## 18. Integration test: Domain → MongoDB

The first real persistence test creates a domain aggregate:

```java
Enrollment enrollment =
        new Enrollment(
                new EnrollmentId("integration-001"),
                new CourseId(10L),
                new StudentName("Rubén")
        );
```

The real adapter persists it:

```java
mongoEnrollmentAdapter.save(enrollment);
```

The real repository reads it back:

```java
Optional<EnrollmentDocument> persisted =
        enrollmentRepository.findById("integration-001");
```

Assertions verify:

```text
document exists
courseId = 10
studentName = Rubén
status = PENDING
```

This validates the whole persistence direction:

```text
Domain Enrollment
        ↓
MongoEnrollmentAdapter
        ↓
EnrollmentDocument
        ↓
Spring Data MongoDB
        ↓
real MongoDB container
        ✅
```

---

## 19. Integration test: MongoDB → Domain

The opposite direction is also tested.

A persistence document is stored directly:

```java
EnrollmentDocument document =
        new EnrollmentDocument(
                "integration-001",
                10L,
                "Rubén",
                EnrollmentStatus.PENDING.name()
        );

enrollmentRepository.save(document);
```

Then the real adapter loads domain objects:

```java
List<Enrollment> enrollmentList =
        mongoEnrollmentAdapter.findAll();
```

The returned domain aggregate is checked using value objects and the domain status.

This validates:

```text
real MongoDB container
        ↓
EnrollmentDocument
        ↓
MongoEnrollmentAdapter
        ↓
Enrollment.rehydrate(...)
        ↓
Domain Enrollment
        ✅
```

So the current integration tests cover both directions:

```text
Domain → Persistence ✅
Persistence → Domain ✅
```

---

## 20. Unit tests vs integration tests

### Unit test

```text
CreateEnrollmentService
        ↓
fake/mock ports
```

Characteristics:

```text
very fast
no Spring context
no Docker
no network
no real database
isolates one unit
```

### Mongo integration test

```text
MongoEnrollmentAdapter
        ↓
Spring Data MongoDB
        ↓
MongoDBContainer
```

Characteristics:

```text
real Spring Mongo configuration
real Mongo driver
real database
slower than unit tests
higher confidence in persistence integration
```

Neither replaces the other.

---

## 21. Current test architecture

```text
Domain
│
├── EnrollmentTest
│   └── pure unit test
│
Application
│
├── CreateEnrollmentServiceTest
│   └── hand-written fake ports
│
├── CreateEnrollmentServiceMockitoTest
│   └── Mockito
│
├── FindEnrollmentsServiceTest
│   └── hand-written fake port
│
└── FindEnrollmentsServiceMockitoTest
    └── Mockito

Adapters
│
├── CourseRestAdapterTest
│   └── isolated adapter test
│
├── MongoEnrollmentAdapterTest
│   └── isolated adapter mapping test
│
└── MongoEnrollmentAdapterIntegrationTest
    ├── @DataMongoTest
    ├── Testcontainers
    └── real MongoDB
```

---

## 22. Useful commands

Run all Enrollment Service tests:

```powershell
.\mvnw test
```

Run only the Mongo integration test:

```powershell
.\mvnw -Dtest=MongoEnrollmentAdapterIntegrationTest test
```

Inspect Testcontainers dependencies:

```powershell
.\mvnw dependency:tree | findstr /I testcontainers
```

Inspect MongoDB test dependencies:

```powershell
.\mvnw dependency:tree | findstr /I mongodb-test
```

---

## 23. Current checkpoint

Completed so far:

```text
Pure unit tests                          ✅
JUnit 5                                  ✅
Hand-written fake ports                  ✅
Mockito fundamentals                     ✅
@Mock                                    ✅
@InjectMocks                             ✅
when / thenReturn                        ✅
thenAnswer                               ✅
verify                                   ✅
never                                    ✅
ArgumentCaptor                           ✅
CreateEnrollmentService Mockito tests    ✅
FindEnrollmentsService Mockito tests     ✅
Integration-test fundamentals            ✅
@DataMongoTest                           ✅
Testcontainers                           ✅
MongoDBContainer                         ✅
@ServiceConnection                       ✅
Real Mongo persistence test              ✅
Real Mongo rehydration test              ✅
```

Still upcoming:

```text
Repository-specific tests                ⏳
REST API tests                            ⏳
HTTP/security integration tests           ⏳
Additional failure-path integration tests ⏳
```

---

## 24. Interview questions

**What is the difference between a mock and a fake?**

A fake is a lightweight working implementation created for testing. A mock is a test double usually created by a mocking framework and can be configured and verified.

**What is stubbing?**

Stubbing defines what a mock should return when a method is called.

```java
when(port.findAll()).thenReturn(enrollments);
```

**What is interaction verification?**

It checks whether a collaborator was called.

```java
verify(port).findAll();
```

**Why use `never()`?**

To prove that an interaction must not happen on a failure path.

```java
verify(savePort, never()).save(any());
```

**What is `ArgumentCaptor` used for?**

It captures the actual argument passed to a mock so the test can inspect it.

**Why use Testcontainers?**

It provides reproducible real infrastructure for automated tests without depending on a manually configured local database.

**Why not mock MongoDB in this integration test?**

Because the purpose of the test is to verify the real interaction between the adapter, Spring Data MongoDB, the Mongo driver and MongoDB itself.

**Why use `@DataMongoTest` instead of `@SpringBootTest` here?**

The persistence adapter needs only MongoDB-related Spring configuration. A narrower test slice avoids loading unrelated infrastructure.

**What does `@ServiceConnection` do?**

It lets Spring Boot automatically use connection details from a supported Testcontainer.

**Why clean the repository in `@BeforeEach`?**

To keep tests independent and prevent data leakage between test cases.

**Are Mockito tests integration tests?**

No. A test that isolates a class using mocked collaborators is still a unit test.

**Does a Testcontainers test replace unit tests?**

No. Unit tests give fast isolated feedback; Testcontainers integration tests give confidence that real infrastructure integration works.

---

## 25. Next testing milestone

The next step is to continue outward from the persistence adapter:

```text
Repository tests
        ↓
REST API tests
        ↓
HTTP + security integration
```

The testing phase remains **in progress**.
