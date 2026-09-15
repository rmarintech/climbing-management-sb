# KAFKA COMMANDS — CLIMBING MANAGEMENT

=========================================

## 1. Kafka Docker Environment

Current Docker networking:

```text
Windows / IntelliJ
        ↓
localhost:8082
        ↓
Kafka broker

Docker containers
        ↓
kafka:18082
        ↓
Kafka broker
```

Start Kafka and the topic initialization service:

```powershell
docker compose up -d kafka kafka-init
```

Check their status:

```powershell
docker compose ps -a
```

Expected:

```text
kafka        Up ... (healthy)
kafka-init   Exited (0)
```

`kafka-init` is a one-shot container.

It:

```text
waits for Kafka
      ↓
creates course-events
      ↓
exits successfully
```

Check Kafka logs:

```powershell
docker compose logs kafka --tail 50
```

---

## 2. List Kafka Topics

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --list
```

Expected:

```text
course-events
```

---

## 3. Describe the Course Events Topic

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

Example:

```text
Topic: course-events
PartitionCount: 1
ReplicationFactor: 1

Partition: 0
Leader: 1
Replicas: 1
Isr: 1
```

Meaning:

```text
course-events
      ↓
Partition 0
      ↓
Leader broker 1
      ↓
Replica broker 1
```

Current learning configuration:

```text
Partitions          = 1
Replication factor  = 1
```

---

## 4. Topic, Partition and Offset

Kafka stores records inside topic partitions.

```text
Topic
  ↓
Partition
  ↓
Ordered append-only log
  ↓
Offsets
```

Example:

```text
course-events
    ↓
Partition 0
    ↓
offset 0 → event A
offset 1 → event B
offset 2 → event C
```

Important:

```text
Ordering is guaranteed inside a partition.
```

An offset identifies the position of a record inside a partition.

---

## 5. Publish a Kafka Record Manually

Start the console producer:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Then enter a valid event:

```json
{"eventType":"COURSE_CREATED","courseId":1,"name":"Sport Climbing","difficulty":"EASY"}
```

Press:

```text
Enter
```

to publish.

Exit with:

```text
Ctrl+C
```

Conceptually:

```text
Console Producer
      ↓
course-events
      ↓
Partition 0
      ↓
Kafka stores record
```

---

## 6. Consume Records Manually

Read all records from the beginning:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events `
    --from-beginning
```

Exit with:

```text
Ctrl+C
```

---

## 7. Consume Key, Partition and Offset

Kafka 4.x uses `--formatter-property`.

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events `
    --from-beginning `
    --formatter-property print.key=true `
    --formatter-property key.separator=" | " `
    --formatter-property print.partition=true `
    --formatter-property print.offset=true
```

Example:

```text
Partition:0 | Offset:0 | null | {...}
Partition:0 | Offset:1 | 5 | {...}
```

The manually produced message may have:

```text
key = null
```

The Spring producer uses:

```text
key = courseId
```

Example:

```text
Kafka Record
├── key   = "5"
└── value = CourseCreatedEvent JSON
```

---

## 8. Spring Course Producer

The Course application publishes:

```text
CourseCreatedEvent
```

to:

```text
course-events
```

The routing match is the topic name.

Infrastructure:

```text
kafka-init
    ↓
creates "course-events"
```

Producer:

```java
KafkaTemplate.send(
    "course-events",
    key,
    event
    );
```

Consumer:

```java
@KafkaListener(topics = "course-events")
```

Therefore:

```text
"course-events"
      ↓
shared Kafka routing name
```

Kafka itself does not know that the payload is a Java `CourseCreatedEvent`.

Kafka stores:

```text
key   → bytes
value → bytes
```

The producer and consumer agree on the event contract.

---

## 9. Create a Course Through REST

```powershell
$body = @{
    name = "Kafka Sport Climbing"
    price = 120.0
    difficulty = "EASY"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/jpa/courses" `
    -ContentType "application/json" `
    -Body $body
```

Expected Course Service flow:

```text
POST /jpa/courses
        ↓
CourseServiceJpa
        ↓
PostgreSQL save
        ↓
CourseCreatedEvent
        ↓
CourseEventProducer
        ↓
KafkaTemplate
        ↓
course-events
```

Example producer log:

```text
CourseCreatedEvent published. courseId=6, partition=0, offset=0
```

---

## 10. Enrollment Kafka Consumer

Enrollment uses:

```properties
spring.kafka.consumer.group-id=enrollment-service
```

and consumes:

```text
course-events
```

Example consumer log:

```text
CourseCreatedEvent consumed. courseId=6, name=Kafka Multi Pitch, difficulty=MEDIUM
```

Complete asynchronous flow:

```text
POST /jpa/courses
        ↓
Course Service
        ↓
PostgreSQL
        ↓
CourseCreatedEvent
        ↓
Kafka Producer
        ↓
course-events
        ↓
Kafka
        ↓
Enrollment Consumer
        ↓
Enrollment Service
```

---

## 11. Inspect Consumer Group

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

Example:

```text
GROUP              TOPIC          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
enrollment-service course-events  0          3               3               0
```

Meaning:

```text
CURRENT-OFFSET
    ↓
next offset the consumer group expects to process

LOG-END-OFFSET
    ↓
current end of the Kafka partition

LAG
    ↓
number of records still waiting to be consumed
```

Example:

```text
CURRENT-OFFSET = 3
LOG-END-OFFSET = 3
LAG = 0
```

means:

```text
Kafka has progressed to position 3
        ↓
Enrollment has also progressed to position 3
        ↓
nothing pending
        ↓
LAG = 0
```

Important:

```text
CURRENT-OFFSET = 3
```

does not mean offset `3` has already been consumed.

It means:

```text
offset 0 ✅
offset 1 ✅
offset 2 ✅

next expected position = 3
```

---

## 12. Consumer Offline / Lag Experiment

Stop the Enrollment Service.

Then publish new Course events while Enrollment is offline.

Kafka continues storing them:

```text
Enrollment DOWN
      ↓
Course events continue arriving
      ↓
Kafka stores events
      ↓
consumer offset does not move
      ↓
LAG increases
```

Inspect:

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

Example:

```text
CURRENT-OFFSET = 1
LOG-END-OFFSET = 3
LAG = 2
```

Meaning:

```text
offset 0 ✅ consumed

offset 1 ⏳ pending
offset 2 ⏳ pending
```

Restart Enrollment.

Kafka resumes from the committed consumer-group position:

```text
Enrollment starts
      ↓
reads pending events
      ↓
committed offset advances
      ↓
LAG returns to 0
```

Example:

```text
CURRENT-OFFSET = 3
LOG-END-OFFSET = 3
LAG = 0
```

---

## 13. REST vs Kafka Failure Behaviour

Synchronous REST:

```text
REST dependency down
        ↓
request normally fails now
```

Asynchronous Kafka:

```text
Kafka consumer down
        ↓
events remain stored in Kafka
        ↓
consumer starts later
        ↓
continues from committed offset
```

This is one of the major benefits of asynchronous communication.

---

## 14. Consumer Group Mental Model

```text
Kafka Topic
    ↓
records remain stored

Consumer Group
    ↓
tracks processing progress
```

During an outage:

```text
Enrollment DOWN
      ↓
events accumulate
      ↓
LAG increases
```

After recovery:

```text
Enrollment UP
      ↓
continues from committed offset
      ↓
catches up
      ↓
LAG = 0
```

---

## 15. Delete the Topic

For learning/testing only:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --delete `
    --topic course-events
```

Recreate it through the Compose initialization service:

```powershell
docker compose run --rm kafka-init
```

Verify:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

Deleting and recreating the topic also resets its records and offsets associated with that topic.

---

## 16. Current Kafka Architecture

```text
                   Course Service
                        │
                        │ CourseCreatedEvent
                        ▼
                  Kafka Producer
                        │
                        ▼
                  course-events
                        │
                        ▼
                     Kafka
                        │
                        ▼
                Consumer Group
               enrollment-service
                        │
                        ▼
               Enrollment Service
```

Current status:

```text
Kafka broker                ✅
KRaft                       ✅
Topic                       ✅
Partition                   ✅
Offsets                     ✅
Console Producer            ✅
Console Consumer            ✅
Spring Producer             ✅
JSON serialization          ✅
Spring Consumer             ✅
Consumer Group              ✅
Committed offsets           ✅
Lag / recovery              ✅
```

Next:

```text
Consumer Groups
      ↓
Multiple consumers
      ↓
Partition assignment
      ↓
Parallelism
```

---

## 17. Spring Kafka Producer Configuration

The Course application publishes `CourseCreatedEvent` records.

Common producer serialization belongs in the root `application.properties`:

```properties
#### KAFKA PRODUCER

# Kafka record key: String -> bytes
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer

# Spring Boot 4 / Spring Kafka 4 use the Jackson 3 serializer.
# Kafka record value: CourseCreatedEvent -> JSON -> bytes
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer

# Do not couple consumers to the producer's Java package/class through type headers.
spring.kafka.producer.properties[spring.json.add.type.headers]=false
```

Environment-specific broker addresses remain in the profile files.

Local / IntelliJ:

```properties
spring.kafka.bootstrap-servers=localhost:8082
```

Docker:

```properties
spring.kafka.bootstrap-servers=kafka:18082
```

The resulting producer flow is:

```text
CourseCreatedEvent
        ↓
JacksonJsonSerializer
        ↓
JSON bytes
        ↓
KafkaTemplate
        ↓
course-events
```

---

## 18. Spring Kafka Consumer Configuration

The Enrollment Service uses its own local representation of `CourseCreatedEvent`.

Common consumer configuration:

```properties
#### KAFKA CONSUMER

spring.kafka.consumer.group-id=enrollment-service

spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer

# Deserialize JSON into Enrollment's own event class.
spring.kafka.consumer.properties[spring.json.value.default.type]=com.rubenmarin.enrollmentservice.event.CourseCreatedEvent

# Ignore producer Java type headers.
spring.kafka.consumer.properties[spring.json.use.type.headers]=false

# Trust the local event package used by the consumer.
spring.kafka.consumer.properties[spring.json.trusted.packages]=com.rubenmarin.enrollmentservice.event

# New consumer groups begin from the earliest available record while learning.
spring.kafka.consumer.auto-offset-reset=earliest
```

The listener subscribes by topic name:

```java
@KafkaListener(topics = "course-events")
public void consume(CourseCreatedEvent event) {
    // process event
}
```

---

## 19. Producer and Consumer Event Contracts

The Course Service and Enrollment Service intentionally use different Java classes for the same compatible event schema.

```text
Course Service
com.rubenmarin.climbingmanagementsb.event.CourseCreatedEvent
        ↓
JSON
        ↓
Kafka
        ↓
Enrollment Service
com.rubenmarin.enrollmentservice.event.CourseCreatedEvent
```

The services agree on the wire fields, not on a shared Java model package.

Current event shape:

```json
{
  "eventId": "b3d988fc-63a7-4265-8327-ac38756e09be",
  "occurredAt": "2026-09-15T09:29:31.536713100Z",
  "courseId": 5,
  "name": "Kafka Sport Climbing",
  "price": 120.0,
  "difficulty": "EASY"
}
```

The Enrollment representation currently models `difficulty` as a `String` rather than importing the Course Service's `Difficulty` enum.

```text
Bad coupling

Enrollment Service
      ↓
imports Course Service model.Difficulty
      ↓
consumer depends on producer internals

Better

Producer contract
      ↓
JSON event
      ↓
Kafka
      ↓
consumer-owned representation
```

---

## 20. Topic Name Is the Routing Match

`kafka-init`, the producer and the consumer are connected by the shared topic name:

```text
kafka-init
--topic course-events

Course producer
KafkaTemplate.send("course-events", key, event)

Enrollment consumer
@KafkaListener(topics = "course-events")
```

Kafka does not know that `course-events` contains a Java `CourseCreatedEvent`.

Kafka stores records as bytes:

```text
Kafka Record
├── key   → byte[]
└── value → byte[]
```

The event schema is an application-level contract.

---

## 21. Spring Boot 4 / Jackson 3 Note

With Spring Boot 4 / Spring Kafka 4, use the Jackson 3 serializer/deserializer classes:

```text
JacksonJsonSerializer
JacksonJsonDeserializer
```

Do not use the older Jackson 2 serializer for this setup:

```text
JsonSerializer
```

Using the old serializer caused a runtime error looking for the Jackson 2 class:

```text
com.fasterxml.jackson.databind.JavaType
```

The working Boot 4 path is:

```text
CourseCreatedEvent
        ↓
JacksonJsonSerializer
        ↓
Jackson 3
        ↓
Kafka
```

---

## 22. KIP-848 Console Message

Kafka 4.3 may print:

```text
The consumer rebalance protocol (KIP-848) is production-ready!
Set group.protocol=consumer to try it out.
```

This is informational, not an error.

The current course still uses the configured consumer group normally while consumer-group rebalancing and partition assignment are studied in the next milestone.

---

## 23. Current Learning Position

Completed:

```text
Synchronous vs asynchronous     ✅
Kafka / KRaft                   ✅
Topics                          ✅
Partitions                      ✅
Offsets                         ✅
Console producer / consumer     ✅
Spring producer                 ✅
Spring consumer                 ✅
JSON serialization              ✅
Consumer group basics           ✅
Committed offsets               ✅
Lag and recovery                ✅
```

Current:

```text
Consumer Groups                 🚧
      ↓
Multiple consumers
      ↓
Partition assignment
      ↓
Parallelism
```

Still to cover:

```text
Event contract evolution
Retry strategy
Idempotency
```
