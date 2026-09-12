# Climbing Management API — Backend

Detailed application and database documentation for the Climbing Management API.

**Main README:** [README.md](README.md)  
**Infrastructure:** [README-INFRASTRUCTURE.md](README-INFRASTRUCTURE.md)

---

# 📚 Current Features
## 🌐 REST API

The project provides course management operations using REST.

PostgreSQL / JPA
`GET     /jpa/courses`
`GET     /jpa/courses/{id}`
`POST    /jpa/courses`
`PUT     /jpa/courses/{id}`
`DELETE  /jpa/courses/{id}`

Additional query endpoints:

`GET /jpa/courses/most-expensive`
`GET /jpa/courses/difficulty/{difficulty}`
`GET /jpa/courses/difficulty/{difficulty}/price/{price}`
## 🗄️ Spring Data JPA

The PostgreSQL persistence layer uses:

- [x] Spring Data JPA
`↓`
- [x] Hibernate
`↓`
JDBC
`↓`
- [x] PostgreSQL
## Derived Queries

Queries can be generated automatically from repository method names.

**Example:**

`List<CourseEntity> findByDifficulty(Difficulty difficulty);`
## JPQL

Custom queries can be written using the entity model:

`@Query("""`
`SELECT c`
FROM CourseEntity c
WHERE c.difficulty = :difficulty
AND c.price < :price
""")
`List<CourseEntity> searchCourses(`
`@Param("difficulty") Difficulty difficulty,`
`@Param("price") Double price`
`);`
## 🔄 Transactions

The project demonstrates Spring transaction management using:

- [x] @Transactional
- [x] Transaction propagation
- [x] REQUIRED
- [x] REQUIRES_NEW
Transaction rollback
Checked vs unchecked exceptions
rollbackFor

**Example:**

- [x] @Transactional
`public void updateCourse(...) {`
...
`}`

The project demonstrates how transaction propagation affects independent operations and rollback behavior.

## 🧠 Hibernate Dirty Checking

The project demonstrates Hibernate's Persistence Context and dirty checking.

An existing managed entity can be modified without explicitly calling save():

existing.setPrice(courseRecord.price());

Hibernate detects the change and generates the required UPDATE statement when the transaction commits.

## 🔐 Concurrency Control

Both major JPA locking strategies are demonstrated.

## Optimistic Locking

Implemented using:

`@Version`
`private Long version;`

Hibernate uses the version field to detect concurrent modifications.

**Conceptually:**

`UPDATE courses`
SET ...
version = ?
WHERE id = ?
AND version = ?

If another transaction has already modified the entity, the version no longer matches and Hibernate detects the conflict.

Optimistic locking is useful when concurrent conflicts are relatively uncommon and we don't want to hold database locks while processing requests.

## Pessimistic Locking

Implemented using:

`@Lock(LockModeType.PESSIMISTIC_WRITE)`

This requests a database-level lock when selecting the entity.

With PostgreSQL, Hibernate generated SQL similar to:

`SELECT ...`
FROM courses
WHERE id = ?
FOR NO KEY UPDATE;

The exact SQL depends on the database and Hibernate dialect.

Pessimistic locking is useful when concurrent modifications must be serialized at the database level.

## ⚠️ Global Exception Handling

The API uses @RestControllerAdvice to centralize exception handling.

Current handlers include:

MethodArgumentNotValidException
`↓`
HTTP 400

CourseNotFoundException
`↓`
HTTP 404

Unexpected Exception
`↓`
HTTP 500

Unexpected exceptions are logged internally using SLF4J while exposing only a safe generic message to the client.

**Example:**

`{`
"timestamp": "2026-09-04T10:52:28.462609Z",
"status": 500,
"message": "An unexpected error occurred"
`}`

This prevents internal implementation details and stack traces from being exposed through the API.

## 🧪 Bean Validation

The API uses Jakarta Bean Validation.

**Example:**

`@NotBlank`
`private String name;`

`@NotNull`
`@Positive`
`private Double price;`

`@NotNull`
`private Difficulty difficulty;`

Invalid requests are converted into structured 400 BAD_REQUEST responses.

**Example:**

`{`
"timestamp": "...",
"status": 400,
"message": "Validation failed",
"errors": {
"name": "must not be blank",
"price": "must be greater than 0"
`}`
`}`
## 🍃 MongoDB

MongoDB was introduced as a second persistence technology alongside PostgreSQL.

The MongoDB layer uses:

- [x] Spring Data MongoDB
`↓`
MongoRepository / MongoTemplate
`↓`
- [x] MongoDB

Database:

climbing_management

Collections:

courses
enrollments
## 📦 MongoDB Documents

Courses are represented using a MongoDB document:

`@Document(collection = "courses")`
`public class CourseMongoDocument {`
...
`}`

The MongoDB model is intentionally separate from the JPA entity.

This demonstrates the difference between:

Relational persistence
`↓`
`@Entity`

and:

MongoDB persistence
`↓`
`@Document`
## 🔎 MongoRepository

The project uses MongoRepository for standard CRUD operations.

Examples include:

save(...)
findById(...)
findAll(...)
deleteById(...)
existsById(...)
count()

Spring Data provides the implementation automatically.

## 🔍 MongoDB Queries

The project demonstrates several querying approaches.

## Derived Queries

**Example:**

`List<CourseMongoDocument> findByDifficulty(`
Difficulty difficulty
`);`

More complex example:

`List<CourseMongoDocument> findByDifficultyAndPriceLessThan(`
Difficulty difficulty,
Double price
`);`
## Custom @Query

**Example:**

`@Query("{ 'difficulty': ?0, 'price': { $lt: ?1 } }")`
`List<CourseMongoDocument> findCoursesByDifficultyAndMaxPriceQuery(`
Difficulty difficulty,
Double price
`);`

MongoDB operators demonstrated include:

$gt
$gte
$lt
$lte
$eq
$ne
$in
$nin
$regex
## 🔎 Text Search

The project also demonstrates derived text queries:

`List<CourseMongoDocument> findByNameContainingIgnoreCase(`
String name
`);`

This provides a simple case-insensitive substring search.

## 📄 Pagination and Sorting

MongoDB queries support Spring's Pageable abstraction.

**Example:**

`Page<CourseMongoDocument> findByDifficultyAndPriceLessThanEqual(`
Difficulty difficulty,
Double price,
Pageable pageable
`);`

**Example request:**

`GET /mongo/courses/search?difficulty=MEDIUM&maxPrice=160&page=1&size=2&sort=price,asc`

The API returns a Page containing:

Current page
Page size
Total elements
Total pages
Content
## 🔄 DTOs

The MongoDB API uses separate DTOs for requests and responses.

Request DTO
`public class CourseMongoRequestDto {`
...
`}`

Used for:

POST
PUT
Response DTO
`public class CourseMongoResponseDto {`
...
`}`

Used to expose API responses without exposing the persistence model directly.

This keeps the API contract independent from the MongoDB document structure.

## 🧩 MongoTemplate

For more complex MongoDB operations, the project uses MongoTemplate.

MongoRepository is ideal for standard CRUD and well-defined repository queries.

MongoTemplate is useful when queries need to be built dynamically.

The project implements a custom repository:

CourseMongoCustomRepository
`↓`
CourseMongoCustomRepositoryImpl
`↓`
- [x] MongoTemplate
## 🔎 Dynamic MongoDB Queries

The project implements a dynamic search where all filters are optional.

Available filters:

name
difficulty
minPrice
maxPrice

**Example:**

`GET /mongo/courses/dynamic-search`
`GET /mongo/courses/dynamic-search?difficulty=MEDIUM`
`GET /mongo/courses/dynamic-search?minPrice=100&maxPrice=160`
`GET /mongo/courses/dynamic-search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc`

The query is dynamically constructed using:

Query
Criteria
- [x] MongoTemplate

**Example:**

`if (difficulty != null) {`
query.addCriteria(
Criteria.where("difficulty").is(difficulty)
`);`
`}`

`if (minPrice != null) {`
query.addCriteria(
Criteria.where("price").gte(minPrice)
`);`
`}`

This avoids creating a separate repository method for every possible combination of filters.

## 📊 MongoDB Indexes

The project demonstrates MongoDB indexing and compound indexes.

A compound index is configured using:

`@CompoundIndex(`
name = "difficulty_price_idx",
def = "{'difficulty': 1, 'price': 1}"
`)`

The index supports queries involving:

difficulty
price

MongoDB index creation is enabled through:

`spring.data.mongodb.auto-index-creation=true`

The project also covers why compound index field order matters and how index design should be based on real query patterns.

## 🔬 MongoDB explain()

The project demonstrates how to analyze query execution.

**Example:**

`db.courses.find({`
difficulty: "MEDIUM",
price: { $lt: 160 }
}).explain("executionStats")

Important execution statistics include:

IXSCAN
COLLSCAN
totalKeysExamined
totalDocsExamined
nReturned
executionTimeMillis

This demonstrates how to verify whether MongoDB is using the expected index instead of scanning the entire collection.

## 📈 MongoDB Aggregation

The project demonstrates MongoDB aggregation pipelines using MongoTemplate.

**Example aggregation:**

`db.courses.aggregate([`
`{`
$match: {
price: { $gte: 100 }
`}`
},
`{`
$group: {
_id: "$difficulty",
averagePrice: { $avg: "$price" },
courseCount: { $sum: 1 }
`}`
},
`{`
$project: {
_id: 0,
difficulty: "$_id",
averagePrice: 1,
courseCount: 1
`}`
},
`{`
$sort: {
averagePrice: -1
`}`
`}`
`])`

The equivalent Spring Data implementation uses:

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
`)`
`);`

The pipeline demonstrates:

$match
`↓`
$group
`↓`
$project
`↓`
$sort

This demonstrates server-side filtering, grouping, calculations, projection and sorting.

The results are mapped to:

CourseDifficultyStatsDto
## 🔗 MongoDB $lookup

The project also demonstrates relationships between MongoDB collections.

Two collections are used:

courses
enrollments

The project uses MongoDB's:

$lookup

operator to perform a join-like operation.

**Example:**

`db.courses.aggregate([`
`{`
$lookup: {
from: "enrollments",
localField: "_id",
foreignField: "courseId",
as: "enrollments"
`}`
`}`
`])`

**Conceptually:**

courses._id
`│`
`│ matches`
`▼`
enrollments.courseId

The result is mapped to:

CourseWithEnrollmentsDto

The $lookup behaves similarly to a LEFT OUTER JOIN in relational databases: courses without matching enrollments are still returned with an empty enrollments array.

The project also demonstrates the importance of matching MongoDB BSON types when using $lookup, such as:

ObjectId ≠ String
## 🧱 Embedded vs Referenced Documents

The project demonstrates the conceptual difference between two common MongoDB modelling strategies.

Embedded

Related data is stored directly inside the parent document.

`{`
"name": "Via Ferrata",
"students": [
`{`
"name": "John"
`}`
`]`
`}`
Referenced

Related data is stored in another collection and linked using an identifier.

courses
`↓`
courseId
`↓`
enrollments

The appropriate strategy depends on:

Data relationships
Read patterns
Update patterns
Document size
Data growth
Whether related data is shared
## 💳 MongoDB Transactions

The project demonstrates MongoDB transactions using Spring's @Transactional.

MongoDB transactions require a replica set or sharded cluster.

For local development, the project uses a single-node replica set, which is sufficient for transaction support.

**Example:**

Replica Set: rs0

127.0.0.1:27017
`│`
`▼`
PRIMARY

The project demonstrates rollback after a failed operation:

- [x] @Transactional
`public void createCourseWithFailure() {`
CourseMongoDocument course =
`new CourseMongoDocument(`
"Transactional Course",
200.0,
Difficulty.HARD
`);`

courseMongoRepository.save(course);

`throw new RuntimeException(`
"Simulated transaction failure"
`);`
`}`

It also demonstrates a transaction spanning multiple MongoDB collections:

Transaction
`│`
`├── courses`
`│`
`└── enrollments`

If the transaction fails after both writes, both operations are rolled back.

This demonstrates MongoDB transaction behavior and the importance of configuring MongoDB as a replica set for transactional workloads.

## 🌐 MongoDB Endpoints

Current MongoDB course endpoints include:

`GET    /mongo/courses`
`GET    /mongo/courses/{id}`
`POST   /mongo/courses`
`PUT    /mongo/courses/{id}`
`DELETE /mongo/courses/{id}`

Query examples:

`GET /mongo/courses/difficulty/{difficulty}`

`GET /mongo/courses/difficulty-lt-price/{difficulty}/{price}`

`GET /mongo/courses/difficulty-max-price-query?difficulty=EASY&price=100`

`GET /mongo/courses/minimum-price?price=90`

`GET /mongo/courses/difficultyIn?difficulties=EASY,MEDIUM`

`GET /mongo/courses/difficultyIn-query?difficulties=EASY,MEDIUM`

`GET /mongo/courses/difficultyNotIn-query?difficulties=HARD,MEDIUM`

`GET /mongo/courses/name-contains?name=ferr`

`GET /mongo/courses/search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc`

`GET /mongo/courses/dynamic-search`

`GET /mongo/courses/difficulty-stats`

`GET /mongo/courses/with-enrollments`
