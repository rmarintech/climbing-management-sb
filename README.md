# Climbing Management API

A backend application built with **Java 21 and Spring Boot** to manage climbing courses.

The project is designed as a practical **Senior Backend Java** learning and portfolio project, demonstrating modern enterprise backend development, REST APIs, persistence, transaction management, concurrency control, relational and NoSQL databases, dynamic queries, aggregation, containerization, CI/CD, Kubernetes orchestration, security practices, and clean layered architecture.

The application is being developed incrementally, introducing technologies and architectural patterns commonly used in enterprise Java applications.

---

# 🚀 Tech Stack

## Backend

* **Java 21**
* **Spring Boot 4.1**
* **Spring Web**
* **Spring Data JPA**
* **Spring Data MongoDB**
* **Spring Boot Actuator**
* **Hibernate**
* **Jakarta Bean Validation**
* **SLF4J / Logging**

## Databases

* **PostgreSQL 17**
* **MongoDB 7**

## Development Tools

* **Maven 3.9+**
* **Git / GitHub**
* **IntelliJ IDEA**
* **Postman**

## Infrastructure

* **Docker**
* **Docker Compose**
* **Docker Secrets**
* **Docker Healthchecks**
* **Docker multi-stage builds**
* **Trivy vulnerability scanning**
* **Kubernetes**
* **Helm** *(in progress)*

## CI/CD

* **GitHub Actions**
* **Maven CI builds**
* **Automated tests**
* **GitHub Actions service containers**
* **JAR artifact publishing**
* **GitHub Container Registry (GHCR)**
* **Docker image build and publishing**
* **Immutable Docker image tags**
* **Continuous Delivery**
* **Production deployment approval**

## Kubernetes

* **Kubernetes Deployments**
* **Pods**
* **ReplicaSets**
* **Services**
* **ClusterIP**
* **Kubernetes DNS / service discovery**
* **ConfigMaps**
* **Secrets**
* **Namespaces**
* **Startup probes**
* **Readiness probes**
* **Liveness probes**
* **Rolling updates**
* **Multiple application replicas**
* **EndpointSlices**
* **PersistentVolumes**
* **PersistentVolumeClaims**
* **StorageClasses**
* **Persistent PostgreSQL storage**
* **Resource requests and limits**
* **Metrics Server**
* **Horizontal Pod Autoscaler (HPA)**
* **Ingress**
* **NGINX Ingress Controller**
* **kubectl port-forward**
* **Helm chart for deployment** *(in progress)*

---

# 🏗️ Architecture

The application follows a layered architecture:

```text
                    REST API
                       │
                       ▼
                  Controller
                       │
                       ▼
                    Service
                       │
              ┌────────┴────────┐
              ▼                 ▼
          Repository       Custom Repository
              │                 │
              ▼                 ▼
         Spring Data        MongoTemplate
              │                 │
       ┌──────┴──────┐          │
       ▼             ▼          ▼
  PostgreSQL      MongoDB    MongoDB
```

The application is also deployed as a Kubernetes workload:

```text
                         Kubernetes
                              │
                         Ingress
                              │
                              ▼
                    Application Service
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
                 App Pod             App Pod
                    │                   │
                    └─────────┬─────────┘
                              │
                 ┌────────────┴────────────┐
                 ▼                         ▼
          PostgreSQL Service          MongoDB Service
                 │                         │
                 ▼                         ▼
          PostgreSQL Pod              MongoDB Pod
                 │
                 ▼
             PVC / PV
```

### Main layers

**Controller**

Exposes the REST API and handles HTTP requests and responses.

**Service**

Contains business logic and transaction boundaries.

**Repository**

Provides data access through Spring Data repositories.

**Custom Repository**

Used when standard repository methods are not sufficient, for example dynamic MongoDB queries and aggregation pipelines.

**Entity / Document**

Represents the persistence model for PostgreSQL and MongoDB.

**DTO / Record**

Defines the API contract independently from the persistence model.

---

# 📚 Current Features

## 🌐 REST API

The project provides course management operations using REST.

### PostgreSQL / JPA

```text
GET     /jpa/courses
GET     /jpa/courses/{id}
POST    /jpa/courses
PUT     /jpa/courses/{id}
DELETE  /jpa/courses/{id}
```

Additional query endpoints:

```text
GET /jpa/courses/most-expensive
GET /jpa/courses/difficulty/{difficulty}
GET /jpa/courses/difficulty/{difficulty}/price/{price}
```

---

# 🗄️ Spring Data JPA

The PostgreSQL persistence layer uses:

```text
Spring Data JPA
       ↓
Hibernate
       ↓
JDBC
       ↓
PostgreSQL
```

## Derived Queries

Queries can be generated automatically from repository method names.

Example:

```java
List<CourseEntity> findByDifficulty(Difficulty difficulty);
```

## JPQL

Custom queries can be written using the entity model:

```java
@Query("""
    SELECT c
    FROM CourseEntity c
    WHERE c.difficulty = :difficulty
    AND c.price < :price
    """)
List<CourseEntity> searchCourses(
        @Param("difficulty") Difficulty difficulty,
        @Param("price") Double price
);
```

---

# 🔄 Transactions

The project demonstrates Spring transaction management using:

* `@Transactional`
* Transaction propagation
* `REQUIRED`
* `REQUIRES_NEW`
* Transaction rollback
* Checked vs unchecked exceptions
* `rollbackFor`

Example:

```java
@Transactional
public void updateCourse(...) {
    ...
}
```

The project demonstrates how transaction propagation affects independent operations and rollback behavior.

---

# 🧠 Hibernate Dirty Checking

The project demonstrates Hibernate's Persistence Context and dirty checking.

An existing managed entity can be modified without explicitly calling `save()`:

```java
existing.setPrice(courseRecord.price());
```

Hibernate detects the change and generates the required `UPDATE` statement when the transaction commits.

---

# 🔐 Concurrency Control

Both major JPA locking strategies are demonstrated.

## Optimistic Locking

Implemented using:

```java
@Version
private Long version;
```

Hibernate uses the version field to detect concurrent modifications.

Conceptually:

```sql
UPDATE courses
SET ...
    version = ?
WHERE id = ?
AND version = ?
```

If another transaction has already modified the entity, the version no longer matches and Hibernate detects the conflict.

Optimistic locking is useful when concurrent conflicts are relatively uncommon and we don't want to hold database locks while processing requests.

---

## Pessimistic Locking

Implemented using:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

This requests a database-level lock when selecting the entity.

With PostgreSQL, Hibernate generated SQL similar to:

```sql
SELECT ...
FROM courses
WHERE id = ?
FOR NO KEY UPDATE;
```

The exact SQL depends on the database and Hibernate dialect.

Pessimistic locking is useful when concurrent modifications must be serialized at the database level.

---

# ⚠️ Global Exception Handling

The API uses `@RestControllerAdvice` to centralize exception handling.

Current handlers include:

```text
MethodArgumentNotValidException
        ↓
HTTP 400

CourseNotFoundException
        ↓
HTTP 404

Unexpected Exception
        ↓
HTTP 500
```

Unexpected exceptions are logged internally using SLF4J while exposing only a safe generic message to the client.

Example:

```json
{
    "timestamp": "2026-09-04T10:52:28.462609Z",
    "status": 500,
    "message": "An unexpected error occurred"
}
```

This prevents internal implementation details and stack traces from being exposed through the API.

---

# 🧪 Bean Validation

The API uses Jakarta Bean Validation.

Example:

```java
@NotBlank
private String name;

@NotNull
@Positive
private Double price;

@NotNull
private Difficulty difficulty;
```

Invalid requests are converted into structured `400 BAD_REQUEST` responses.

Example:

```json
{
    "timestamp": "...",
    "status": 400,
    "message": "Validation failed",
    "errors": {
        "name": "must not be blank",
        "price": "must be greater than 0"
    }
}
```

---

# 🍃 MongoDB

MongoDB was introduced as a second persistence technology alongside PostgreSQL.

The MongoDB layer uses:

```text
Spring Data MongoDB
        ↓
MongoRepository / MongoTemplate
        ↓
MongoDB
```

Database:

```text
climbing_management
```

Collections:

```text
courses
enrollments
```

---

# 📦 MongoDB Documents

Courses are represented using a MongoDB document:

```java
@Document(collection = "courses")
public class CourseMongoDocument {
    ...
}
```

The MongoDB model is intentionally separate from the JPA entity.

This demonstrates the difference between:

```text
Relational persistence
        ↓
@Entity
```

and:

```text
MongoDB persistence
        ↓
@Document
```

---

# 🔎 MongoRepository

The project uses `MongoRepository` for standard CRUD operations.

Examples include:

```java
save(...)
findById(...)
findAll(...)
deleteById(...)
existsById(...)
count()
```

Spring Data provides the implementation automatically.

---

# 🔍 MongoDB Queries

The project demonstrates several querying approaches.

## Derived Queries

Example:

```java
List<CourseMongoDocument> findByDifficulty(
        Difficulty difficulty
);
```

More complex example:

```java
List<CourseMongoDocument> findByDifficultyAndPriceLessThan(
        Difficulty difficulty,
        Double price
);
```

---

## Custom `@Query`

Example:

```java
@Query("{ 'difficulty': ?0, 'price': { $lt: ?1 } }")
List<CourseMongoDocument> findCoursesByDifficultyAndMaxPriceQuery(
        Difficulty difficulty,
        Double price
);
```

MongoDB operators demonstrated include:

```text
$gt
$gte
$lt
$lte
$eq
$ne
$in
$nin
$regex
```

---

# 🔎 Text Search

The project also demonstrates derived text queries:

```java
List<CourseMongoDocument> findByNameContainingIgnoreCase(
        String name
);
```

This provides a simple case-insensitive substring search.

---

# 📄 Pagination and Sorting

MongoDB queries support Spring's `Pageable` abstraction.

Example:

```java
Page<CourseMongoDocument> findByDifficultyAndPriceLessThanEqual(
        Difficulty difficulty,
        Double price,
        Pageable pageable
);
```

Example request:

```text
GET /mongo/courses/search?difficulty=MEDIUM&maxPrice=160&page=1&size=2&sort=price,asc
```

The API returns a `Page` containing:

* Current page
* Page size
* Total elements
* Total pages
* Content

---

# 🔄 DTOs

The MongoDB API uses separate DTOs for requests and responses.

### Request DTO

```java
public class CourseMongoRequestDto {
    ...
}
```

Used for:

```text
POST
PUT
```

### Response DTO

```java
public class CourseMongoResponseDto {
    ...
}
```

Used to expose API responses without exposing the persistence model directly.

This keeps the API contract independent from the MongoDB document structure.

---

# 🧩 MongoTemplate

For more complex MongoDB operations, the project uses `MongoTemplate`.

`MongoRepository` is ideal for standard CRUD and well-defined repository queries.

`MongoTemplate` is useful when queries need to be built dynamically.

The project implements a custom repository:

```text
CourseMongoCustomRepository
        ↓
CourseMongoCustomRepositoryImpl
        ↓
MongoTemplate
```

---

# 🔎 Dynamic MongoDB Queries

The project implements a dynamic search where all filters are optional.

Available filters:

```text
name
difficulty
minPrice
maxPrice
```

Example:

```text
GET /mongo/courses/dynamic-search
```

```text
GET /mongo/courses/dynamic-search?difficulty=MEDIUM
```

```text
GET /mongo/courses/dynamic-search?minPrice=100&maxPrice=160
```

```text
GET /mongo/courses/dynamic-search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc
```

The query is dynamically constructed using:

```java
Query
Criteria
MongoTemplate
```

Example:

```java
if (difficulty != null) {
    query.addCriteria(
        Criteria.where("difficulty").is(difficulty)
    );
}

if (minPrice != null) {
    query.addCriteria(
        Criteria.where("price").gte(minPrice)
    );
}
```

This avoids creating a separate repository method for every possible combination of filters.

---

# 📊 MongoDB Indexes

The project demonstrates MongoDB indexing and compound indexes.

A compound index is configured using:

```java
@CompoundIndex(
    name = "difficulty_price_idx",
    def = "{'difficulty': 1, 'price': 1}"
)
```

The index supports queries involving:

```text
difficulty
price
```

MongoDB index creation is enabled through:

```properties
spring.data.mongodb.auto-index-creation=true
```

The project also covers why **compound index field order matters** and how index design should be based on real query patterns.

---

# 🔬 MongoDB `explain()`

The project demonstrates how to analyze query execution.

Example:

```javascript
db.courses.find({
    difficulty: "MEDIUM",
    price: { $lt: 160 }
}).explain("executionStats")
```

Important execution statistics include:

```text
IXSCAN
COLLSCAN
totalKeysExamined
totalDocsExamined
nReturned
executionTimeMillis
```

This demonstrates how to verify whether MongoDB is using the expected index instead of scanning the entire collection.

---

# 📈 MongoDB Aggregation

The project demonstrates MongoDB aggregation pipelines using `MongoTemplate`.

Example aggregation:

```javascript
db.courses.aggregate([
    {
        $match: {
            price: { $gte: 100 }
        }
    },
    {
        $group: {
            _id: "$difficulty",
            averagePrice: { $avg: "$price" },
            courseCount: { $sum: 1 }
        }
    },
    {
        $project: {
            _id: 0,
            difficulty: "$_id",
            averagePrice: 1,
            courseCount: 1
        }
    },
    {
        $sort: {
            averagePrice: -1
        }
    }
])
```

The equivalent Spring Data implementation uses:

```java
Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(
                Criteria.where("price").gte(100)
        ),
        Aggregation.group("difficulty")
                .avg("price").as("averagePrice")
                .count().as("courseCount"),
        Aggregation.project()
                .and("_id").as("difficulty")
                .and("averagePrice").as("averagePrice")
                .and("courseCount").as("courseCount")
                .andExclude("_id"),
        Aggregation.sort(
                Sort.Direction.DESC,
                "averagePrice"
        )
);
```

The pipeline demonstrates:

```text
$match
   ↓
$group
   ↓
$project
   ↓
$sort
```

This demonstrates server-side filtering, grouping, calculations, projection and sorting.

The results are mapped to:

```java
CourseDifficultyStatsDto
```

---

# 🔗 MongoDB `$lookup`

The project also demonstrates relationships between MongoDB collections.

Two collections are used:

```text
courses
enrollments
```

The project uses MongoDB's:

```text
$lookup
```

operator to perform a join-like operation.

Example:

```javascript
db.courses.aggregate([
    {
        $lookup: {
            from: "enrollments",
            localField: "_id",
            foreignField: "courseId",
            as: "enrollments"
        }
    }
])
```

Conceptually:

```text
courses._id
      │
      │ matches
      ▼
enrollments.courseId
```

The result is mapped to:

```java
CourseWithEnrollmentsDto
```

The `$lookup` behaves similarly to a **LEFT OUTER JOIN** in relational databases: courses without matching enrollments are still returned with an empty `enrollments` array.

The project also demonstrates the importance of matching MongoDB BSON types when using `$lookup`, such as:

```text
ObjectId ≠ String
```

---

# 🧱 Embedded vs Referenced Documents

The project demonstrates the conceptual difference between two common MongoDB modelling strategies.

### Embedded

Related data is stored directly inside the parent document.

```json
{
    "name": "Via Ferrata",
    "students": [
        {
            "name": "John"
        }
    ]
}
```

### Referenced

Related data is stored in another collection and linked using an identifier.

```text
courses
    ↓
courseId
    ↓
enrollments
```

The appropriate strategy depends on:

* Data relationships
* Read patterns
* Update patterns
* Document size
* Data growth
* Whether related data is shared

---

# 💳 MongoDB Transactions

The project demonstrates MongoDB transactions using Spring's `@Transactional`.

MongoDB transactions require a **replica set or sharded cluster**.

For local development, the project uses a **single-node replica set**, which is sufficient for transaction support.

Example:

```text
Replica Set: rs0

127.0.0.1:27017
       │
       ▼
    PRIMARY
```

The project demonstrates rollback after a failed operation:

```java
@Transactional
public void createCourseWithFailure() {
    CourseMongoDocument course =
        new CourseMongoDocument(
            "Transactional Course",
            200.0,
            Difficulty.HARD
        );

    courseMongoRepository.save(course);

    throw new RuntimeException(
        "Simulated transaction failure"
    );
}
```

It also demonstrates a transaction spanning multiple MongoDB collections:

```text
Transaction
    │
    ├── courses
    │
    └── enrollments
```

If the transaction fails after both writes, both operations are rolled back.

This demonstrates MongoDB transaction behavior and the importance of configuring MongoDB as a replica set for transactional workloads.

---

# 🌐 MongoDB Endpoints

Current MongoDB course endpoints include:

```text
GET    /mongo/courses
GET    /mongo/courses/{id}
POST   /mongo/courses
PUT    /mongo/courses/{id}
DELETE /mongo/courses/{id}
```

Query examples:

```text
GET /mongo/courses/difficulty/{difficulty}

GET /mongo/courses/difficulty-lt-price/{difficulty}/{price}

GET /mongo/courses/difficulty-max-price-query?difficulty=EASY&price=100

GET /mongo/courses/minimum-price?price=90

GET /mongo/courses/difficultyIn?difficulties=EASY,MEDIUM

GET /mongo/courses/difficultyIn-query?difficulties=EASY,MEDIUM

GET /mongo/courses/difficultyNotIn-query?difficulties=HARD,MEDIUM

GET /mongo/courses/name-contains?name=ferr

GET /mongo/courses/search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc

GET /mongo/courses/dynamic-search

GET /mongo/courses/difficulty-stats

GET /mongo/courses/with-enrollments
```

---

# 🐳 Docker

Docker containerizes the complete backend environment and its infrastructure.

The application can run as a multi-container stack using Docker Compose:

```text
                    Docker Compose
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
       Spring Boot    PostgreSQL      MongoDB
          │
          │
     port 8080
```

PostgreSQL and MongoDB communicate with the application through the Docker Compose network using service names:

```text
postgres:5432
mongo:27017
```

The application exposes port `8080` to the host.

---

# Dockerfile

The application uses a **multi-stage Docker build**.

```text
Build stage
    │
    ├── Maven
    ├── JDK 21
    ├── Dependency resolution
    └── Application compilation
             │
             ▼
        application JAR
             │
             ▼
Runtime stage
    │
    ├── Java 21 JRE
    ├── Alpine Linux
    └── application JAR
```

The Maven build environment is not included in the final runtime image.

This reduces:

* Image size
* Attack surface
* Number of unnecessary production dependencies

The dependency layer is also separated from the source-code layer to improve Docker build-cache reuse.

---

# 🔐 Docker Security

The container is hardened using several production-oriented practices.

## Non-root user

The Spring Boot application does not run as root.

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring

USER spring
```

The container was verified to run as:

```text
spring
```

## Read-only root filesystem

The application container uses:

```yaml
read_only: true
```

Temporary writable storage is provided through:

```yaml
tmpfs:
  - /tmp
```

This reduces the ability of a compromised application to modify the container filesystem.

---

# 🔑 Docker Secrets

Database credentials are not passed to the Spring Boot application as environment variables.

Instead, Docker Secrets are used.

Conceptually:

```text
Docker Secret
     │
     ├──────────────► PostgreSQL
     │
     └──────────────► Spring Boot
```

PostgreSQL receives the secret through:

```text
/run/secrets/postgres_password
```

The Spring Boot application receives it through:

```text
/run/secrets/spring.datasource.password
```

Spring Boot imports the mounted secret using:

```properties
spring.config.import=optional:configtree:/run/secrets/
```

This maps the mounted filename to:

```text
spring.datasource.password
```

The database password is therefore not exposed as an application environment variable.

---

# 🌐 Docker Networking

Docker Compose provides an internal DNS service.

Containers communicate using service names rather than `localhost`.

For example:

```text
Spring Boot
     │
     ├── postgres:5432
     │
     └── mongo:27017
```

Inside the Spring Boot container:

```text
localhost
```

refers to the Spring Boot container itself.

It does **not** refer to PostgreSQL or MongoDB.

This distinction is essential when troubleshooting containerized applications.

---

# ❤️ Docker Healthchecks

Spring Boot Actuator is used to expose application health information.

The health endpoint is:

```text
GET /actuator/health
```

Example response:

```json
{
    "groups": [
        "liveness",
        "readiness"
    ],
    "status": "UP"
}
```

Docker uses the endpoint as its container healthcheck:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 20s
```

The `start_period` prevents normal application startup time from immediately causing the container to become unhealthy.

---

# 🩺 Health Probes

The project distinguishes between:

```text
Liveness
    ↓
Is the application alive?

Readiness
    ↓
Is the application ready to receive traffic?

Startup
    ↓
Has the application finished starting?
```

These concepts are used directly by the Kubernetes deployment.

---

# 🛠️ Docker Troubleshooting

The project includes hands-on troubleshooting exercises covering:

```text
docker compose ps
docker compose logs
docker compose exec
docker inspect
```

The troubleshooting workflow is:

```text
docker compose ps
        ↓
Is the container running?
        ↓
docker compose logs
        ↓
What is the application reporting?
        ↓
docker compose exec app sh
        ↓
Can we inspect the container from inside?
        ↓
docker inspect
        ↓
What configuration was actually applied?
```

Docker DNS can also be verified from inside the application container:

```bash
getent hosts postgres
getent hosts mongo
```

A deliberate failure was introduced by changing the PostgreSQL hostname from:

```text
postgres
```

to:

```text
localhost
```

The resulting failure demonstrated that `localhost` inside a container refers to the container itself rather than another Compose service.

The issue was diagnosed through application logs, container inspection and Docker DNS verification.

---

# 🔍 Docker Vulnerability Scanning

The project uses **Trivy** to scan Docker images for known vulnerabilities.

Example:

```bash
trivy image climbing-management-sb-app:latest
```

The project demonstrated the difference between vulnerabilities in:

```text
Application dependencies
        vs
Base operating-system packages
```

A vulnerability scan initially detected critical vulnerabilities in:

```text
org.apache.tomcat.embed:tomcat-embed-core
```

The dependency was traced through the Maven dependency tree:

```text
Spring Boot
    ↓
spring-boot-starter-tomcat
    ↓
tomcat-embed-core
```

The Tomcat version was explicitly overridden to the fixed release.

The application JAR subsequently reported:

```text
CRITICAL: 0
```

Remaining operating-system vulnerabilities were identified separately as Alpine base-image findings.

This demonstrates a practical vulnerability-management workflow:

```text
Scan
  ↓
Identify severity
  ↓
Locate dependency
  ↓
Determine fixed version
  ↓
Update dependency
  ↓
Rebuild image
  ↓
Rescan
```

---

# 🧪 Docker + Spring Boot Configuration

Docker-specific Spring configuration is activated using:

```yaml
SPRING_PROFILES_ACTIVE: docker
```

The Docker profile uses Compose service names:

```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/${APP_POSTGRES_DB}

spring.mongodb.uri=mongodb://mongo:27017/${APP_MONGO_DB}
```

The configuration flow is:

```text
.env
 │
 ├── ENV_POSTGRES_DB
 ├── ENV_POSTGRES_USER
 ├── ENV_MONGO_DB
 └── ENV_APP_PORT
        │
        ▼
Docker Compose
        │
        ▼
Spring Boot environment
        │
        ▼
application-docker.properties
```

Secrets follow a separate path:

```text
Docker Secret
      │
      ▼
/run/secrets/
      │
      ▼
Spring Boot configtree
      │
      ▼
spring.datasource.password
```

---

# ☸️ Kubernetes

The application and its database dependencies have been deployed to a local Kubernetes cluster running through **Docker Desktop Kubernetes**.

The Kubernetes environment currently consists of:

```text
                    Kubernetes Cluster
                           │
             ┌─────────────┼─────────────┐
             │             │             │
             ▼             ▼             ▼
       Spring Boot     PostgreSQL     MongoDB
       Deployment      Deployment     Deployment
          │               │             │
       2+ Pods           1 Pod          1 Pod
          │               │             │
          ▼               ▼             ▼
       Service          Service       Service
       :8080            :5432         :27017
```

The application is exposed externally through an Ingress layer:

```text
Client
   │
   ▼
NGINX Ingress Controller
   │
   ▼
Ingress rule
   │
   ▼
climbing-management-service
   │
   ├──► App Pod #1
   │
   └──► App Pod #2
```

---

# 📦 Kubernetes Deployments

The application is deployed using a Kubernetes `Deployment`.

The baseline configuration uses:

```yaml
replicas: 2
```

The application Deployment is also managed by an HPA with:

```text
Minimum replicas: 2
Maximum replicas: 5
CPU target: 70%
```

This means the Deployment normally starts with two replicas but Kubernetes can automatically increase the number of Pods when CPU utilization exceeds the configured target.

Conceptually:

```text
                   Deployment
                       │
                  HPA controls
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
        Minimum: 2          Maximum: 5
             │
             ▼
       Spring Boot Pods
```

The Deployment manages the Pods through a ReplicaSet.

If an application Pod is deleted or fails, Kubernetes creates a replacement to maintain the desired replica count.

This demonstrates Kubernetes **self-healing** and **desired state management**.

---

# 🌐 Kubernetes Services

The application is exposed internally through:

```text
climbing-management-service
```

The Service uses:

```yaml
type: ClusterIP
```

and exposes:

```text
8080
```

The Service selects Pods using:

```yaml
selector:
  app: climbing-management
```

Conceptually:

```text
Service
climbing-management-service:8080
            │
      ┌─────┴─────┐
      ▼           ▼
   App Pod      App Pod
     :8080        :8080
```

The Service provides a stable network endpoint while Pods remain ephemeral.

Kubernetes dynamically maintains the Service's EndpointSlice based on matching and Ready Pods.

---

# 🔎 Kubernetes Service Discovery

Kubernetes provides internal DNS-based service discovery.

The Spring Boot application connects to PostgreSQL using:

```text
postgres:5432
```

and MongoDB using:

```text
mongo:27017
```

These names resolve to Kubernetes Services rather than directly to Pod IP addresses.

Conceptually:

```text
Spring Boot Pod
      │
      ├── postgres:5432
      │       ↓
      │   PostgreSQL Service
      │       ↓
      │   PostgreSQL Pod
      │
      └── mongo:27017
              ↓
          MongoDB Service
              ↓
          MongoDB Pod
```

This is one of the key differences between container-level networking and Kubernetes service discovery.

---

# 🗂️ Kubernetes Namespaces

The project introduces Kubernetes **Namespaces** to logically isolate the application and its resources from other workloads and the cluster's default namespace.

Namespaces provide:

```text
Logical isolation
        ↓
Separate scope for names, ConfigMaps, Secrets, Deployments, Services
```

The application's Deployments, Services, ConfigMaps and Secrets are scoped to a dedicated namespace rather than living in `default`.

This mirrors how real-world clusters typically separate workloads (e.g. by team, environment, or application) and prepares the manifests for multi-environment deployments (`dev`, `staging`, `production`) later on.

Resources can be inspected within the namespace using:

```bash
kubectl get pods -n climbing-management
kubectl get all -n climbing-management
```

---

# ⚙️ Kubernetes ConfigMap

Non-sensitive application configuration is stored in:

```text
climbing-management-config
```

The ConfigMap contains values such as:

```text
SPRING_PROFILES_ACTIVE
APP_NAME
LOG_LEVEL
APP_MONGO_DB
APP_POSTGRES_USER
APP_POSTGRES_DB
```

The application Deployment imports the ConfigMap using:

```yaml
envFrom:
  - configMapRef:
      name: climbing-management-config
```

This separates configuration from the container image.

---

# 🔐 Kubernetes Secrets

Sensitive configuration is stored in:

```text
climbing-management-secret
```

The database password is mounted into the application Pod as a file:

```text
/run/secrets/spring.datasource.password
```

The application does not require the password to be exposed as an environment variable.

The Deployment also disables automatic ServiceAccount token mounting:

```yaml
automountServiceAccountToken: false
```

because the application does not need to communicate with the Kubernetes API.

This follows the principle of least privilege.

---

# 🩺 Kubernetes Health Probes

The Spring Boot Actuator endpoints are used by Kubernetes:

```text
/actuator/health
/actuator/health/readiness
/actuator/health/liveness
```

The Deployment configures:

### Startup probe

Determines whether the application has completed startup.

```text
Startup
   ↓
Application initialization
```

### Readiness probe

Determines whether the Pod should receive traffic.

If readiness fails:

```text
Pod
  ↓
removed from Service endpoints
```

The container is not necessarily restarted.

### Liveness probe

Determines whether the container should be restarted.

If liveness repeatedly fails:

```text
Liveness failure
       ↓
Kubernetes restarts container
```

Interview summary:

> **Liveness determines whether Kubernetes should restart the container. Readiness determines whether the Pod should receive traffic. Startup probes protect slow-starting applications from being restarted by liveness before initialization has completed.**

---

# 🔄 Kubernetes Rolling Updates

The application Deployment supports rolling updates.

When a new application version is deployed, Kubernetes creates new Pods while gradually replacing the old Pods.

Conceptually:

```text
Old version
   │
   ├── Pod A
   └── Pod B
        ↓
Rolling update
        ↓
   ┌────┴────┐
   ▼         ▼
New Pod   New Pod
```

This reduces application downtime during deployments.

The Deployment also maintains revision history, allowing previous versions to be restored using Kubernetes rollout commands.

---

# 💾 Kubernetes Persistent Storage

PostgreSQL is configured with persistent storage so that database data survives Pod recreation.

The storage architecture is:

```text
PostgreSQL Pod
      │
      ▼
PersistentVolumeClaim
      │
      ▼
PersistentVolume
      │
      ▼
StorageClass
      │
      ▼
Dynamically provisioned storage
```

The PostgreSQL PVC requests:

```text
Access mode:
ReadWriteOnce

Storage:
requested by the PostgreSQL workload
```

The `ReadWriteOnce` access mode is appropriate for the single-node local PostgreSQL setup used in this project.

The important Kubernetes storage concepts are:

```text
StorageClass
    ↓
Defines how storage is provisioned

PersistentVolume
    ↓
Represents provisioned storage

PersistentVolumeClaim
    ↓
Application request for storage

Pod
    ↓
Mounts the PVC
```

This separates the application's storage requirement from the underlying storage implementation.

---

# 📊 Kubernetes Resource Requests and Limits

The application Deployment defines CPU and memory resources.

Current application configuration:

```text
CPU request:     250m
Memory request:  512Mi

CPU limit:       500m
Memory limit:    1Gi
```

Conceptually:

```text
Request
   ↓
Resources reserved / used for scheduling decisions

Limit
   ↓
Maximum resource consumption allowed by the container
```

The CPU request is especially important for the HPA because CPU utilization is calculated relative to the configured CPU request.

For this application:

```text
CPU request = 250m
HPA target   = 70%

250m × 70% = 175m
```

Therefore approximately `175m` CPU utilization per Pod corresponds to the 70% HPA target.

Resource requests also allow Kubernetes to make better scheduling decisions across nodes.

---

# 📈 Kubernetes Metrics Server

The project uses **Metrics Server** to provide resource utilization metrics to Kubernetes.

Metrics can be inspected using:

```bash
kubectl top nodes
```

and:

```bash
kubectl top pods
```

Example conceptual output:

```text
NAME                         CPU(cores)   MEMORY(bytes)
climbing-management-xxxxx    120m         350Mi
climbing-management-yyyyy    95m          340Mi
```

Metrics Server is required for the HPA to make CPU-based scaling decisions.

The architecture is:

```text
Kubelet
   │
   ▼
Metrics Server
   │
   ▼
Kubernetes Metrics API
   │
   ▼
HPA
```

---

# 📈 Kubernetes Horizontal Pod Autoscaler

The application uses a Kubernetes **Horizontal Pod Autoscaler**.

The HPA configuration is:

```text
Target:
climbing-management Deployment

Minimum replicas:
2

Maximum replicas:
5

CPU target:
70%
```

Conceptually:

```text
                  Metrics Server
                       │
                       ▼
                      HPA
                       │
                 CPU utilization
                       │
                       ▼
                 Application
                 Deployment
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
          App Pods           App Pods
```

When CPU utilization increases above the target, the HPA increases the number of application replicas.

When utilization decreases, Kubernetes can reduce the number of replicas, respecting the configured minimum.

The HPA manages the Deployment's replica count rather than creating Pods directly.

A simplified scaling calculation is:

```text
desired replicas =
current replicas × current utilization / target utilization
```

For example:

```text
2 replicas
146% CPU utilization
70% target

2 × 146 / 70
≈ 4.17
```

The HPA therefore needs approximately 5 replicas, subject to Kubernetes rounding and the configured maximum.

During load testing, the application demonstrated automatic scaling from:

```text
2 replicas
    ↓
4 replicas
    ↓
5 replicas
```

This demonstrates practical Kubernetes horizontal scaling.

---

# 🌐 Kubernetes Ingress

The application is exposed through a Kubernetes **Ingress**.

Ingress provides Layer 7 HTTP/HTTPS routing.

The project uses:

```text
NGINX Ingress Controller
```

The request flow is:

```text
Client
   │
   ▼
NGINX Ingress Controller
   │
   ▼
Ingress rule
   │
   ▼
climbing-management-service:8080
   │
   ├──► App Pod #1
   │
   └──► App Pod #2
```

The Ingress resource contains the host:

```text
climbing-management.local
```

and routes traffic to:

```text
climbing-management-service:8080
```

The Ingress definition is stored in:

```text
k8s/app-ingress.yaml
```

Conceptually:

```text
Host:
climbing-management.local

Path:
/

Backend:
climbing-management-service:8080
```

Ingress is responsible for HTTP routing, while the Kubernetes Service remains responsible for stable internal access to the application Pods.

---

# 🚪 NGINX Ingress Controller

The project uses the NGINX Ingress Controller to implement the Kubernetes Ingress resource.

The controller watches Kubernetes Ingress resources and configures NGINX accordingly.

The distinction is:

```text
Ingress
   ↓
Kubernetes routing configuration

Ingress Controller
   ↓
Actual component implementing the routing
```

The project installs the NGINX Ingress Controller and verifies that the controller Pod is running.

---

# 🧪 Kubernetes Ingress Local Testing

The Ingress was tested locally through a Kubernetes port-forward.

The controller Service is forwarded to the local machine:

```bash
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8081:80
```

The Ingress routing can then be tested with:

```bash
curl.exe -H "Host: climbing-management.local" http://localhost:8081/actuator/health
```

Expected response:

```text
up
```

The complete request path is:

```text
Windows host
     │
     ▼
localhost:8081
     │
     ▼
kubectl port-forward
     │
     ▼
NGINX Ingress Controller
     │
     ▼
Ingress rule
     │
     ▼
climbing-management-service
     │
     ▼
Spring Boot Pod
     │
     ▼
/actuator/health
```

No Windows hosts-file modification is required for this local test because the HTTP `Host` header is supplied explicitly.

The port-forward is a **development and testing mechanism**, not a production ingress architecture.

---

# 🔌 Kubernetes Local Testing Without Ingress

The application can also be accessed directly through its Service using:

```bash
kubectl port-forward service/climbing-management-service 8080:8080
```

The request path becomes:

```text
localhost:8080
      │
      ▼
kubectl port-forward
      │
      ▼
Kubernetes Service
      │
      ├──► App Pod #1
      │
      └──► App Pod #2
```

Health can then be tested with:

```text
http://localhost:8080/actuator/health
```

This is useful for debugging the Service and application independently of the Ingress layer.

---

# 🧩 Kubernetes Database Services

PostgreSQL is deployed internally as:

```text
PostgreSQL Deployment
        ↓
PostgreSQL Service
        ↓
postgres:5432
        ↓
PostgreSQL Pod
        ↓
PVC
```

MongoDB is deployed internally as:

```text
MongoDB Deployment
        ↓
MongoDB Service
        ↓
mongo:27017
        ↓
MongoDB Pod
```

Both database Services use `ClusterIP`, keeping the databases internal to the Kubernetes cluster.

PostgreSQL is backed by persistent storage through a PVC.

---

# ⎈ Helm

The project is introducing a **Helm chart** to package and deploy the application to Kubernetes, replacing raw `kubectl apply` manifests with a templated, versioned release process.

The chart is being built to deploy the application using the **immutable commit-SHA Docker image tags** published to GHCR during CI, rather than the mutable `latest` tag:

```text
GHCR image
ghcr.io/rmarintech/climbing-management-sb:<commit-sha>
        │
        ▼
   Helm values
        │
        ▼
   Helm release
        │
        ▼
Kubernetes Deployment
```

This keeps the deployed version traceable back to the exact commit that produced it, and is a step toward a repeatable, promotable release process (`helm upgrade`/`helm rollback`) instead of manually editing manifests.

This is an **active, in-progress** area of the project — see the Roadmap below.

---

# 🔄 CI/CD with GitHub Actions

The project uses **GitHub Actions** to automate Continuous Integration and Continuous Delivery.

The pipeline is divided conceptually into:

```text
Continuous Integration
        ↓
Build + Test + Package
        ↓
Docker Image
        ↓
GHCR
        ↓
Continuous Delivery
        ↓
Deployment approval
        ↓
Production deployment
```

---

# 🔨 Continuous Integration

The CI workflow is triggered automatically when code is pushed to the `main` branch.

The current pipeline performs:

```text
Git push
    ↓
GitHub Actions
    ↓
Checkout repository
    ↓
Set up Java 21
    ↓
Start PostgreSQL service
    ↓
Start MongoDB service
    ↓
Run Maven build and tests
    ↓
Create JAR
    ↓
Upload JAR artifact
    ↓
Build Docker image
    ↓
Tag Docker image
    ↓
Push image to GHCR
```

The workflow is implemented in:

```text
.github/workflows/ci.yml
```

---

# 🧪 GitHub Actions Service Containers

The CI environment provisions the external services required by the integration tests.

### PostgreSQL

```yaml
image: postgres:17
```

The CI database is created as:

```text
climbing_management
```

### MongoDB

```yaml
image: mongo:8
```

Both services are configured with healthchecks so the workflow can verify that the databases are ready before the application tests run.

This is important because the GitHub-hosted runner does not use the user's local database installations.

---

# 📦 Maven CI Build

The workflow uses the Maven Wrapper:

```bash
./mvnw package
```

The Maven lifecycle used by the project includes:

```text
mvn test
    ↓
compile + run tests

mvn package
    ↓
compile + test + create JAR

mvn verify
    ↓
package + additional verification

mvn install
    ↓
package + install JAR into local Maven repository
```

The CI pipeline currently uses:

```bash
./mvnw package
```

This ensures that the application is compiled, tested and packaged during CI.

---

# 📦 JAR Artifact

After the Maven build completes, the generated JAR is uploaded as a GitHub Actions artifact.

```text
target/*.jar
        ↓
GitHub Actions Artifact
```

The artifact is named:

```text
climbing-management-jar
```

This demonstrates the distinction between:

```text
JAR
 ↓
Application build artifact
```

and:

```text
Docker image
 ↓
Deployable application package
```

---

# 📦 GitHub Container Registry

The Docker image produced by CI is published to **GitHub Container Registry (GHCR)**.

The image repository is:

```text
ghcr.io/rmarintech/climbing-management-sb
```

The pipeline publishes two important tags:

```text
ghcr.io/rmarintech/climbing-management-sb:latest
```

and an immutable commit-based tag:

```text
ghcr.io/rmarintech/climbing-management-sb:<commit-sha>
```

The commit SHA tag identifies the exact source revision used to build the image.

This is preferable for deployments because:

```text
latest
  ↓
Mutable reference

commit SHA
  ↓
Immutable version reference
```

The workflow authenticates to GHCR using the GitHub Actions `GITHUB_TOKEN` with package write permissions.

Conceptually:

```text
GitHub Repository
        │
        ▼
GitHub Actions
        │
        ├── Build
        ├── Test
        └── Package
              │
              ▼
         Docker Image
              │
        ┌─────┴─────┐
        ▼           ▼
      latest     commit SHA
        │           │
        └─────┬─────┘
              ▼
             GHCR
```

The immutable commit-SHA tag is also the tag now being wired into the in-progress **Helm** chart, so Kubernetes deployments reference a precise, traceable image version.

---

# 🚚 Continuous Delivery

The project also demonstrates a Continuous Delivery workflow.

The CD workflow runs after a successful CI workflow.

Conceptually:

```text
CI succeeds
     ↓
CD workflow
     ↓
Production environment
     ↓
Required approval
     ↓
Deployment
```

The production environment uses a GitHub Actions environment with required reviewers.

This demonstrates an important enterprise CI/CD concept:

> **A successful build does not automatically mean that production deployment should happen without an approval or deployment policy.**

The deployment also uses the immutable Docker image commit SHA so that the deployed version can be identified precisely.

---

# 🔄 Build Once, Deploy Many

The CI/CD pipeline follows the principle:

```text
Source code
     ↓
Build once
     ↓
Test once
     ↓
Create artifact/image
     ↓
Deploy the same version
```

The Docker image is built during CI and published to GHCR.

The resulting image can then be promoted through deployment environments without rebuilding the application.

This reduces the risk of:

```text
Build version A
       ↓
Test version A
       ↓
Rebuild
       ↓
Deploy version B
```

Instead:

```text
Build version A
       ↓
Test version A
       ↓
Deploy version A
```

---

# 🗺️ Roadmap

The project is being developed progressively.

## Completed

* [x] Spring Boot project setup
* [x] Java 21 configuration
* [x] REST API
* [x] Layered architecture
* [x] DTOs / Java Records
* [x] Bean Validation
* [x] Global Exception Handling
* [x] SLF4J logging
* [x] PostgreSQL
* [x] Spring Data JPA
* [x] Hibernate
* [x] Derived Queries
* [x] JPQL
* [x] `@Transactional`
* [x] Transaction propagation
* [x] `REQUIRED`
* [x] `REQUIRES_NEW`
* [x] Rollback rules
* [x] Hibernate Dirty Checking
* [x] Optimistic Locking
* [x] Pessimistic Locking
* [x] MongoDB
* [x] Spring Data MongoDB
* [x] MongoRepository
* [x] MongoDB operators
* [x] MongoDB pagination
* [x] MongoDB sorting
* [x] MongoDB DTOs
* [x] MongoDB validation
* [x] MongoDB indexes
* [x] Compound indexes
* [x] MongoDB `explain()`
* [x] MongoTemplate
* [x] Dynamic MongoDB queries
* [x] MongoDB aggregation
* [x] Aggregation DTO mapping
* [x] MongoDB `$lookup`
* [x] Embedded vs referenced document modelling
* [x] MongoDB replica set configuration
* [x] MongoDB transactions
* [x] MongoDB transaction rollback
* [x] Multi-collection MongoDB transactions
* [x] MongoDB interview comparison with PostgreSQL
* [x] Docker fundamentals
* [x] Docker images and containers
* [x] Dockerfile
* [x] Docker image layers and build cache
* [x] Docker port mapping
* [x] Docker environment variables
* [x] Docker volumes
* [x] Docker networks
* [x] PostgreSQL container
* [x] MongoDB container
* [x] Docker Compose
* [x] Multi-container application
* [x] Docker Compose service discovery
* [x] Multi-stage Docker builds
* [x] Alpine-based runtime image
* [x] Non-root container user
* [x] Read-only container filesystem
* [x] Docker tmpfs
* [x] Docker Secrets
* [x] Spring Boot Docker profile
* [x] Spring Boot Actuator
* [x] Docker healthchecks
* [x] Liveness / readiness concepts
* [x] Docker troubleshooting
* [x] Container inspection with `docker inspect`
* [x] Docker DNS troubleshooting
* [x] Trivy vulnerability scanning
* [x] Dependency vulnerability remediation
* [x] GitHub Actions CI
* [x] CI service containers
* [x] Automated Maven build and tests
* [x] JAR artifact publishing
* [x] Docker image build in CI
* [x] GitHub Container Registry
* [x] Docker image publishing to GHCR
* [x] Docker image tagging strategy
* [x] Immutable commit SHA image tags
* [x] Continuous Delivery
* [x] GitHub Actions production environment
* [x] Deployment approval gate
* [x] Kubernetes cluster
* [x] Kubernetes Pods
* [x] Kubernetes Deployments
* [x] Kubernetes ReplicaSets
* [x] Kubernetes Services
* [x] Kubernetes ClusterIP
* [x] Kubernetes service discovery
* [x] Kubernetes DNS
* [x] Kubernetes ConfigMaps
* [x] Kubernetes Secrets
* [x] Kubernetes startup probes
* [x] Kubernetes readiness probes
* [x] Kubernetes liveness probes
* [x] Kubernetes rolling updates
* [x] Kubernetes multiple application replicas
* [x] Kubernetes EndpointSlices
* [x] Kubernetes database Services
* [x] Kubernetes local application testing
* [x] Kubernetes PersistentVolumes
* [x] Kubernetes PersistentVolumeClaims
* [x] Kubernetes StorageClasses
* [x] PostgreSQL persistent storage
* [x] Kubernetes resource requests
* [x] Kubernetes resource limits
* [x] Kubernetes Metrics Server
* [x] Kubernetes `kubectl top`
* [x] Kubernetes Horizontal Pod Autoscaler
* [x] HPA CPU-based scaling
* [x] HPA load testing
* [x] Kubernetes Ingress
* [x] NGINX Ingress Controller
* [x] Ingress routing
* [x] Local Ingress testing
* [x] Kubernetes Namespaces

## Current

* [ ] Helm chart for Kubernetes deployment
* [ ] Helm deployment using immutable commit SHA images

## Upcoming

* [ ] Advanced REST API design
* [ ] Spring Security
* [ ] Unit testing
* [ ] Integration testing
* [ ] Testcontainers
* [ ] Event-driven architecture
* [ ] Messaging / Kafka
* [ ] Microservices
* [ ] DDD
* [ ] Hexagonal Architecture
* [ ] System design
* [ ] Performance and scalability
* [ ] Senior Backend Java interview preparation

---

# 🎯 Project Goals

The main goal of this project is to build a realistic backend application while demonstrating practical knowledge of enterprise Java technologies.

Key areas include:

* REST API design
* Layered architecture
* Separation of responsibilities
* DTO-based API contracts
* Persistence and ORM
* Transaction management
* Transaction propagation
* Rollback behavior
* Hibernate dirty checking
* Optimistic locking
* Pessimistic locking
* Exception handling
* Bean Validation
* Relational databases
* NoSQL databases
* MongoDB data modelling
* MongoDB indexing
* Query performance analysis
* Dynamic queries
* Aggregation pipelines
* Collection joins using `$lookup`
* MongoDB transactions
* Containerization
* Docker Compose
* Docker security
* Docker Secrets
* Healthchecks
* Vulnerability scanning
* CI/CD
* GitHub Actions
* Container registries
* Kubernetes
* Container orchestration
* Service discovery
* Namespaces
* Persistent storage
* Resource management
* Horizontal autoscaling
* Ingress and HTTP routing
* Health probes
* Helm packaging
* Distributed systems
* Messaging
* Microservices
* Domain-driven design
* Clean architecture

The project is also used as a practical learning environment for **Senior Backend Java development and technical interview preparation**.

---

# 🛠️ Running the Project

## Requirements

### Local development

* Java 21
* Maven 3.9+
* PostgreSQL 17
* MongoDB 7+
* Git

### Docker development

* Docker Desktop
* Docker Compose

### Kubernetes development

* Docker Desktop with Kubernetes enabled
* `kubectl`
* `helm` *(for the in-progress Helm chart)*

The recommended development approach is to run infrastructure and application components through Docker Compose or Kubernetes depending on the learning scenario.

Clone the repository:

```bash
git clone https://github.com/rmarintech/climbing-management-sb.git
cd climbing-management-sb
```

---

# Maven

Run the test suite:

```bash
mvn clean test
```

Build the application:

```bash
mvn clean package
```

Run the application locally:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The API will be available at:

```text
http://localhost:8080
```

---

# Docker Compose

Start the complete environment:

```bash
docker compose up -d --build
```

Check the containers:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs app
```

Follow application logs:

```bash
docker compose logs -f app
```

Stop the environment:

```bash
docker compose down
```

The application will be available at:

```text
http://localhost:8080
```

Healthcheck:

```text
http://localhost:8080/actuator/health
```

Expected response:

```json
{
    "groups": [
        "liveness",
        "readiness"
    ],
    "status": "UP"
}
```

---

# ☸️ Kubernetes

Apply the application configuration:

```bash
kubectl apply -f k8s/app-config.yaml
kubectl apply -f k8s/app-secret.yaml
```

Deploy PostgreSQL storage:

```bash
kubectl apply -f k8s/postgres-pvc.yaml
```

Deploy PostgreSQL:

```bash
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml
```

Deploy MongoDB:

```bash
kubectl apply -f k8s/mongo-deployment.yaml
kubectl apply -f k8s/mongo-service.yaml
```

Deploy the application:

```bash
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml
```

Check the cluster:

```bash
kubectl get pods
```

Expected application state:

```text
climbing-management-xxxxx   1/1   Running
climbing-management-xxxxx   1/1   Running
```

Check Services:

```bash
kubectl get services
```

Check EndpointSlices:

```bash
kubectl get endpointslices
```

Check persistent storage:

```bash
kubectl get pvc
kubectl get pv
kubectl get storageclass
```

Check resource usage:

```bash
kubectl top pods
kubectl top nodes
```

Check the application Deployment:

```bash
kubectl get deployment climbing-management
```

Check the HPA:

```bash
kubectl get hpa
```

Check rollout status:

```bash
kubectl rollout status deployment/climbing-management
```

Check namespace-scoped resources:

```bash
kubectl get all -n climbing-management
```

---

## Kubernetes Local API Access

Forward the Kubernetes Service to the local machine:

```bash
kubectl port-forward service/climbing-management-service 8080:8080
```

Then access:

```text
http://localhost:8080
```

Health:

```text
http://localhost:8080/actuator/health
```

Liveness:

```text
http://localhost:8080/actuator/health/liveness
```

Readiness:

```text
http://localhost:8080/actuator/health/readiness
```

---

## Kubernetes Ingress Access

Forward the NGINX Ingress Controller locally:

```bash
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8081:80
```

Then test the Ingress routing:

```bash
curl.exe -H "Host: climbing-management.local" http://localhost:8081/actuator/health
```

Expected response:

```text
up
```

---

# 📮 Example Requests

## Create a PostgreSQL course

```http
POST /jpa/courses
Content-Type: application/json
```

```json
{
    "name": "Sport Climbing",
    "price": 120.0,
    "difficulty": "EASY"
}
```

Example response:

```json
{
    "id": 1,
    "name": "Sport Climbing",
    "price": 120.0,
    "difficulty": "EASY"
}
```

---

## Create a MongoDB course

```http
POST /mongo/courses
Content-Type: application/json
```

```json
{
    "name": "Via Ferrata",
    "price": 90.0,
    "difficulty": "EASY"
}
```

---

## Dynamic MongoDB search

```http
GET /mongo/courses/dynamic-search?difficulty=MEDIUM&minPrice=100&maxPrice=160&page=0&size=2&sort=price,asc
```

---

## MongoDB aggregation

```http
GET /mongo/courses/difficulty-stats
```

Returns statistics grouped by course difficulty.

---

## MongoDB `$lookup`

```http
GET /mongo/courses/with-enrollments
```

Returns courses together with their related enrollments.

---

# 📌 Learning Approach

The project is intentionally developed incrementally.

Each major technology is introduced through:

```text
Theory
   ↓
Implementation
   ↓
Database inspection
   ↓
API testing
   ↓
Concurrency / behaviour testing
   ↓
Troubleshooting
   ↓
Interview questions
   ↓
Code cleanup
   ↓
Git commit
   ↓
CI/CD validation
```

Each major milestone is committed to Git so the repository provides a clear history of the technologies and concepts implemented.

The CI/CD pipeline additionally validates the project automatically after every push to `main`, builds the application, produces the JAR artifact, builds and publishes the Docker image to GHCR, and supports controlled Continuous Delivery.

---

# 👨‍💻 Author

**Rubén Marín**

Backend Java Developer

Technologies explored in this project include:

`Java` · `Spring Boot` · `Spring Data JPA` · `Hibernate` · `PostgreSQL` · `Spring Data MongoDB` · `MongoDB` · `MongoTemplate` · `Docker` · `Docker Compose` · `GitHub Actions` · `GitHub Container Registry` · `Kubernetes` · `Helm`
