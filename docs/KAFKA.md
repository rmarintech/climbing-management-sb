# Apache Kafka / Event-Driven Architecture

This document contains the Kafka theory, architecture, configuration notes, implementation decisions, and learning conclusions for the **Climbing Management** project.

Progress tracking belongs in [`ROADMAP.md`](ROADMAP.md). Command-only reference belongs in [`../cheatsheets/kafka.md`](../cheatsheets/kafka.md).

---

## Synchronous REST vs Asynchronous Kafka

The project uses both synchronous and asynchronous communication because they solve different problems.

```text
REST
Enrollment Service → Course Service
caller waits for an immediate answer

Kafka
Course Service → Kafka → Enrollment Service
producer publishes an event and the consumer can process it later
```

A useful mental model is:

```text
REST  → "Do this / tell me this now"
Kafka → "This happened"
```

Kafka does not replace REST. Enrollment still uses REST to validate Course existence before creating an enrollment, while Course lifecycle events can be communicated asynchronously.

---

## Kafka Docker Environment

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

### Inspect the Docker Kafka environment

```powershell
docker compose ps -a
```

Useful for confirming that:

```text
kafka        → running / healthy
kafka-init   → exited successfully
```

Inspect recent broker logs:

```powershell
docker compose logs kafka --tail 50
```

List the topics currently known by the broker:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --list
```


---

## Describe the Course Events Topic

Example:

```text
Topic: course-events
PartitionCount: 2
ReplicationFactor: 1

Partition: 0
Leader: 1
Replicas: 1
Isr: 1

Partition: 1
Leader: 1
Replicas: 1
Isr: 1
```

Meaning:

```text
course-events
├── Partition 0
│   └── Leader / Replica / ISR: broker 1
│
└── Partition 1
    └── Leader / Replica / ISR: broker 1
```

Current learning configuration:

```text
Partitions          = 2
Replication factor  = 1
```

These fields describe how Kafka stores and replicates a topic partition.

### Partition

A **partition** is one ordered piece of a Kafka topic.

```text
course-events
└── Partition 0
    ├── offset 0
    ├── offset 1
    └── offset 2
```

Kafka guarantees record ordering **within a partition**.

Partitions also provide scalability and consumer parallelism.

### Leader

Every partition has one **leader broker**.

The leader is the broker currently responsible for handling reads and writes for that partition.

```text
Partition 0
    ↓
Leader: Broker 1
```

In our current environment:

```text
Leader: 1
```

means Broker 1 is the leader for Partition 0.

### Replicas

`Replicas` lists all brokers that store that partition.

The leader itself is included in this list.

With:

```text
ReplicationFactor: 1
Replicas: 1
```

we have:

```text
Partition 0
    ↓
Broker 1
    ├── Leader
    └── only replica
```

There is therefore no redundant copy.

In a production-like cluster we might have:

```text
ReplicationFactor: 3
Replicas: 1,2,3
```

meaning three brokers store that partition.

### ISR — In-Sync Replicas

`ISR` means **In-Sync Replicas**.

These are the replicas that are currently sufficiently synchronized with the leader.

Example:

```text
Leader:   1
Replicas: 1,2,3
ISR:      1,2,3
```

means all three replicas are currently in sync.

If Broker 3 falls behind, we could temporarily see:

```text
Leader:   1
Replicas: 1,2,3
ISR:      1,2
```

Broker 3 still belongs to the replica set, but it is currently not in the ISR.

### Current project

Our current Kafka environment now has two partitions:

```text
PartitionCount:     2
ReplicationFactor:  1

Partition: 0
Leader:    1
Replicas:  1
ISR:       1

Partition: 1
Leader:    1
Replicas:  1
ISR:       1
```

So:

```text
course-events
├── Partition 0
│   └── Broker 1
│       ├── Leader
│       ├── Replica
│       └── In-Sync Replica
│
└── Partition 1
    └── Broker 1
        ├── Leader
        ├── Replica
        └── In-Sync Replica
```

Two partitions allow consumer parallelism, but `ReplicationFactor: 1` still means there is no broker redundancy.

### Inspect partitions, leader, replicas and ISR

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

The output is the source for values such as:

```text
PartitionCount
ReplicationFactor
Partition
Leader
Replicas
Isr
```


A useful distinction is:

```text
Replicas
   ↓
brokers that are supposed to contain the partition

ISR
   ↓
replicas currently synchronized enough to participate safely
```
---

## Topic, Partition and Offset

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

## Kafka Record Key, Partition and Offset

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

### Display key, partition and offset

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

This lets you inspect the physical Kafka metadata discussed above rather than only the JSON payload.


---

## Spring Course Producer

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

## Enrollment Kafka Consumer

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

## Inspect Consumer Group

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

### Inspect committed offsets and lag

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

The important columns are:

```text
CURRENT-OFFSET
LOG-END-OFFSET
LAG
```


---

## Consumer Offline / Lag Experiment

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

## REST vs Kafka Failure Behaviour

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

## Consumer Group Mental Model

```text
Kafka Topic
      ↓
Records remain stored

Consumer Group
       ↓
Tracks processing progress
```

During an outage:

```text
Enrollment DOWN
      ↓
Events accumulate
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

## Kafka Architecture

```text
                         Course Service
                              │
                              │ CourseCreatedEvent
                              ▼
                        Kafka Producer
                              │
                              │ key = courseId
                              ▼
                         course-events
                              │
                      ┌───────┴───────┐
                      │               │
                Partition 0      Partition 1
                      │               │
                      └───────┬───────┘
                              │
                       Consumer Group
                      enrollment-service
                              │
                    Enrollment consumer
```

During the consumer-group parallelism experiment, a second Enrollment instance was started temporarily so Kafka could assign one partition to each consumer.

Current status:

```text
Kafka broker                ✅
KRaft                       ✅
Topics                      ✅
Partitions                  ✅
Offsets                     ✅
Console Producer            ✅
Console Consumer            ✅
Spring Producer             ✅
JSON serialization          ✅
Spring Consumer             ✅
Consumer Groups             ✅
Committed offsets           ✅
Lag / recovery              ✅
Partition assignment        ✅
Rebalancing                 ✅
Consumer failover           ✅
Parallel consumption        ✅
Key / partition / offset    ✅
Event contracts             ✅
Schema evolution            ✅
Compatibility / versioning  ✅
Retry strategy              ✅
Dead Letter Topic           ✅
Poison-pill handling        ✅
Idempotency                 ✅
```

Kafka / Event-Driven Architecture is now complete for the current course scope.

Next major phase:

```text
DDD / Bounded Contexts
      ↓
Hexagonal Architecture
      ↓
Ports and Adapters
```

---

## Spring Kafka Producer Configuration

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

## Spring Kafka Consumer Configuration

The Enrollment Service uses its own local representation of `CourseCreatedEvent`.

Common consumer configuration:

```properties
#### KAFKA CONSUMER

spring.kafka.consumer.group-id=enrollment-service

spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

# Wrap deserialization failures so they can be handled by the listener container
# and eventually routed to the DLT.
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

# Actual JSON deserializer used by ErrorHandlingDeserializer.
spring.kafka.consumer.properties[spring.deserializer.value.delegate.class]=org.springframework.kafka.support.serializer.JacksonJsonDeserializer

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
public void consume(ConsumerRecord<String, CourseCreatedEvent> record) {
    CourseCreatedEvent event = record.value();

    log.info(
            "CourseCreatedEvent consumed. key={}, courseId={}, partition={}, offset={}, name={}, difficulty={}",
            record.key(),
            event.courseId(),
            record.partition(),
            record.offset(),
            event.name(),
            event.difficulty()
    );
}
```

Using `ConsumerRecord` exposes the Kafka metadata alongside the deserialized payload:

```text
record.key()
record.partition()
record.offset()
record.value()
```

---

## Producer and Consumer Event Contracts

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
  "eventType": "COURSE_CREATED",
  "eventVersion": 2,
  "sourceService": "course-service",
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

## Topic Name Is the Routing Match

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

## Spring Boot 4 / Jackson 3 Note

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

## Multiple Consumers, Partition Assignment and Parallelism

This milestone was tested with real Enrollment Service instances in the same consumer group.

### One partition and two consumers

Initially:

```text
course-events
└── Partition 0

Consumer group: enrollment-service
├── Enrollment #1
└── Enrollment #2
```

Kafka assigned the only partition to one consumer:

```text
Consumer #1
#PARTITIONS = 1
CURRENT-ASSIGNMENT = course-events:0

Consumer #2
#PARTITIONS = 0
CURRENT-ASSIGNMENT = -
```

Therefore:

```text
Partition 0 → Consumer #1
              Consumer #2 idle
```

The idle consumer was still connected to the group; it simply had no partition to consume.

The key rule is:

```text
inside the same consumer group,
one partition can be owned by only one consumer at a time
```

### Failover and rebalance

When the active consumer was stopped, Kafka detected the group membership change and rebalanced the group.

The previously idle consumer then logged:

```text
partitions assigned: [course-events-0]
```

Conceptually:

```text
Consumer #1 stops
        ↓
group membership changes
        ↓
Kafka rebalances
        ↓
Partition 0 → Consumer #2
```

This demonstrated automatic consumer failover.

### Inspect group members and partition assignments

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

This is the command used to observe:

```text
consumer members
#PARTITIONS
CURRENT-ASSIGNMENT
```

and to verify reassignment after a consumer joins or leaves.


### Two partitions and two consumers

The `course-events` topic was then changed to two partitions:

```text
course-events
├── Partition 0
└── Partition 1
```

When the second consumer joined, Kafka first revoked the existing assignments and then redistributed ownership.

The resulting model was:

```text
Partition 0 → Consumer #1
Partition 1 → Consumer #2
```

or the reverse.

Now both consumers could work in parallel.

The fundamental relationship is:

```text
maximum useful consumers in one group
        ≤
number of partitions assigned to the group
```

For this topic:

```text
2 partitions
    +
2 consumers
    +
same group
    =
2 active consumers
```

### Record distribution is not broadcasting

A single Kafka record is written to exactly one partition.

Two partitions do **not** mean the same event is copied to two consumers.

Instead:

```text
event
  ↓
key = courseId
  ↓
Kafka partitioner
  ↓
one partition
  ↓
consumer that owns that partition
```

Real test results showed different Course IDs being handled by the two Enrollment instances.

Example:

```text
Consumer A
key=20, courseId=20, partition=0, offset=6
key=21, courseId=21, partition=0, offset=7

Consumer B
key=19, courseId=19, partition=1, offset=3
```

This also proves that offsets are local to each partition:

```text
Partition 0
offset 6
offset 7

Partition 1
offset 3
```

There is no single global topic offset.

### Rebalancing

Kafka may rebalance when:

```text
consumer joins
consumer leaves
consumer crashes
consumer restarts
partition count changes
```

During a rebalance, existing assignments may be revoked and Kafka calculates new partition ownership.

This milestone demonstrated:

```text
Consumer Groups          ✅
Partition assignment     ✅
Rebalancing              ✅
Consumer failover        ✅
Parallelism              ✅
Partition-local offsets  ✅
```

The consumer-group milestone was followed by **Event Contracts and schema evolution**, documented below.


---

## Event Contract Versioning and Schema Evolution

The event contract evolved from an initial unversioned shape to an explicit versioned contract.

Current metadata:

```text
eventType     = COURSE_CREATED
eventVersion  = 2
sourceService = course-service
```

### Compatible additive evolution

The producer moved from V1 to V2 by adding `sourceService`. An older Enrollment consumer without that field still deserialized the V2 event successfully because the additional field could be ignored.

```text
New producer V2
      ↓
extra field: sourceService
      ↓
old consumer
      ↓
unknown field ignored
      ↓
works ✅
```

The consumer was then updated to include `sourceService`, and an old V1 event was replayed. The result was:

```text
eventVersion=1
sourceService=null
```

The new consumer could still process the old event.

### Replay an old V1 contract

Start the console producer:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Then paste:

```json
{"eventId":"11111111-1111-1111-1111-111111111111","eventType":"COURSE_CREATED","eventVersion":1,"occurredAt":"2026-09-16T08:00:00Z","courseId":9001,"name":"Kafka Contract V1 Replay","price":170.0,"difficulty":"MEDIUM"}
```

The V2 consumer should deserialize it with:

```text
eventVersion=1
sourceService=null
```


### Breaking schema evolution

A deliberately incompatible event changed `price` from a number to an incompatible string:

```json
"price": "ONE HUNDRED SEVENTY EUROS"
```

The consumer still expected `Double price`, so deserialization failed.

### Reproduce the breaking schema error

Start the console producer:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Then paste the incompatible event:

```json
{"eventId":"22222222-2222-2222-2222-222222222222","eventType":"COURSE_CREATED","eventVersion":3,"sourceService":"course-service","occurredAt":"2026-09-16T09:00:00Z","courseId":9002,"name":"Kafka Broken Contract","price":"ONE HUNDRED SEVENTY EUROS","difficulty":"MEDIUM"}
```


```text
Kafka JSON
      ↓
incompatible field type
      ↓
JacksonJsonDeserializer
      ↓
DeserializationException ❌
```

A useful rule is:

```text
Usually safer
├── add optional fields
└── add fields consumers can ignore

Potentially breaking
├── remove fields
├── rename fields
├── change field types
├── change field meaning
└── add required fields old data cannot provide
```

---

## Error Handling, Retry Strategy and Dead Letter Topic

The breaking schema experiment produced a poison-pill record that could not be deserialized.

The consumer now uses:

```text
ErrorHandlingDeserializer
      ↓
JacksonJsonDeserializer
```

Valid events that fail during listener processing use:

```text
DefaultErrorHandler
      ↓
FixedBackOff(1000 ms, 2 retries)
      ↓
DeadLetterPublishingRecoverer
      ↓
course-events-dlt
```

The Docker Kafka initialization creates two partitions for both the source topic and its DLT:

```text
course-events
├── Partition 0
└── Partition 1

course-events-dlt
├── Partition 0
└── Partition 1
```

### Deserialization failure

Malformed records fail before the listener method and are treated as non-retryable/fatal by default.

```text
bad bytes / incompatible schema
      ↓
ErrorHandlingDeserializer
      ↓
DeserializationException
      ↓
DeadLetterPublishingRecoverer
      ↓
course-events-dlt
```

The DLT preserves diagnostic headers such as original topic, partition, offset, consumer group, exception class, exception message, and stack trace.

### Inspect the Dead Letter Topic

Describe it:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events-dlt
```

Consume DLT records together with their diagnostic headers:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events-dlt `
    --from-beginning `
    --formatter-property print.key=true `
    --formatter-property print.partition=true `
    --formatter-property print.offset=true `
    --formatter-property print.headers=true
```


### Processing failure

A learning experiment deliberately threw a `RuntimeException` inside the listener. With:

```java
new FixedBackOff(1000L, 2L)
```

the observed flow was:

```text
initial attempt
      ↓
    fails
      ↓
    retry 1
      ↓
    fails
      ↓
    retry 2
      ↓
    fails
      ↓
     DLT
```

Two retries means three total processing attempts: one original attempt plus two retries.

### Trigger the processing retry experiment

With the temporary `"Kafka Retry Test"` failure block enabled in `CourseEventConsumer`:

```powershell
$body = @{
    name = "Kafka Retry Test"
    price = 160.0
    difficulty = "MEDIUM"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/jpa/courses" `
    -ContentType "application/json" `
    -Body $body
```

Then watch the Enrollment logs for the initial attempt, the two retries, and final DLT recovery.


A `RetryListener` was added so failed deliveries, successful recovery, and recovery failure are visible in logs.

### DLT and consumer progress

Once a failed record is successfully recovered to the DLT, the original consumer group can advance past that offset. The original record is not deleted from Kafka; the consumer group's committed position moves forward while the failed copy and diagnostics remain in the DLT.

---

## Idempotent Consumer

Kafka can redeliver records, so the consumer must tolerate the same logical event arriving more than once.

The event already contains a unique:

```text
eventId
```

This identifies the event itself and is different from `courseId`, because one Course can eventually produce many events.

Enrollment stores processed event IDs in MongoDB:

```text
processed_kafka_events
```

with:

```text
_id = eventId
```

The document stores `eventId`, `eventType`, and `processedAt`.

Because UUID values are persisted to MongoDB, Enrollment explicitly configures:

```properties
spring.mongodb.representation.uuid=standard
```

Without it, the MongoDB Java driver raised:

```text
CodecConfigurationException:
The uuidRepresentation has not been specified,
so the UUID cannot be encoded.
```

The current learning flow is:

```text
event arrives
      ↓
wasAlreadyProcessed(eventId)?
      ├── yes → skip duplicate
      │
      └── no
           ↓
       process event
           ↓
       markAsProcessed(event)
```

The same `eventId` was published twice. The first delivery was processed and stored; the second was detected and skipped.

### Reproduce the duplicate-event test

Open a console producer:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Paste the exact same event twice:

```json
{"eventId":"33333333-3333-3333-3333-333333333333","eventType":"COURSE_CREATED","eventVersion":2,"sourceService":"course-service","occurredAt":"2026-09-16T13:00:00Z","courseId":9003,"name":"Kafka Idempotency Test","price":175.0,"difficulty":"MEDIUM"}
```

Expected behavior:

```text
first record  → processed
second record → duplicate skipped
```

Inspect the persisted event marker in MongoDB:

```powershell
docker compose exec mongo mongosh
```

Then:

```javascript
use enrollment_management

db.processed_kafka_events.find().pretty()
```

The collection should contain the processed `eventId`.


```text
eventId=33333333-3333-3333-3333-333333333333

offset 8 → processed
offset 9 → duplicate skipped
```

### Production nuance

The current learning flow is not fully atomic because the business work and processed-event marker are separate operations. A stronger Inbox / Idempotent Consumer design would place the business changes and processed-event marker in the same MongoDB transaction so they commit or roll back together.

---

## Kafka Phase Complete

The Kafka phase now covers:

```text
Synchronous vs asynchronous communication
Kafka / KRaft
Topics
Partitions
Offsets
Record keys
Spring producer
Spring consumer
Consumer groups
Committed offsets
Lag and recovery
Partition assignment
Rebalancing and failover
Parallel consumption
Event contracts
Event type / version
Schema evolution
Compatibility testing
Breaking schema changes
ErrorHandlingDeserializer
Retry strategy
Fixed backoff
Dead Letter Topic
Poison-pill handling
Retry diagnostics
EventId-based idempotency
MongoDB processed-event persistence
Duplicate detection and skipping
```


