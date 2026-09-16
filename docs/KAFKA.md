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

## Current Kafka Architecture

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
                  ┌───────────┴───────────┐
                  │                       │
           Enrollment #1           Enrollment #2
```

Current status:

```text
Kafka broker                ✅
KRaft                       ✅
Topic                       ✅
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
```

Next:

```text
Event Contracts             🚧 CURRENT
      ↓
Contract evolution
      ↓
Compatibility / versioning
      ↓
Retry strategy
      ↓
Idempotency
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

## KIP-848 Console Message

Kafka 4.3 may print:

```text
The consumer rebalance protocol (KIP-848) is production-ready!
Set group.protocol=consumer to try it out.
```

This is informational, not an error.

The project continued using the configured consumer group normally while partition assignment, rebalancing, failover, and parallel consumption were tested.

---

## Current Learning Position

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
Consumer groups                 ✅
Committed offsets               ✅
Lag and recovery                ✅
Multiple consumers              ✅
Partition assignment            ✅
Rebalancing                     ✅
Consumer failover               ✅
Parallel consumption            ✅
Partition-local offsets         ✅
ConsumerRecord metadata         ✅
```

Current:

```text
Event Contracts                 🚧
      ↓
Contract evolution
      ↓
Compatibility / versioning
```

Still to cover:

```text
Retry strategy
Idempotency
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

The next Kafka lesson is **Event Contracts and schema evolution**.
