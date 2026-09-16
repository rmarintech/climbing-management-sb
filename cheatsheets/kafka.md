# KAFKA COMMANDS — CLIMBING MANAGEMENT

## Kafka Docker Environment

```powershell
docker compose up -d kafka kafka-init
```

```powershell
docker compose ps -a
```

```powershell
docker compose logs kafka --tail 50
```

---

## List Kafka Topics

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --list
```

---

## Describe the Course Events Topic

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

---

## Describe the Dead Letter Topic

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events-dlt
```

---

## Increase an Existing Topic to 2 Partitions

Use this only when `course-events` already exists with fewer partitions.

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --alter `
    --topic course-events `
    --partitions 2
```

Verify:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

---

## Publish a Kafka Record Manually

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Example V2 event:

```json
{"eventId":"11111111-1111-1111-1111-111111111111","eventType":"COURSE_CREATED","eventVersion":2,"sourceService":"course-service","occurredAt":"2026-09-16T09:00:00Z","courseId":9001,"name":"Manual Kafka Event","price":170.0,"difficulty":"MEDIUM"}
```

Exit:

```text
Ctrl+C
```

---

## Consume Records Manually

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events `
    --from-beginning
```

Exit:

```text
Ctrl+C
```

---

## Consume Key, Partition and Offset

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

---

## Create a Course Through REST

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

---

## Create a Course for Parallel Consumer Testing

```powershell
$body = @{
    name = "test kafka 2 consumers 2 partitions"
    price = 160.0
    difficulty = "MEDIUM"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/jpa/courses" `
    -ContentType "application/json" `
    -Body $body
```

Run the request several times to generate different `courseId` values and observe partition distribution.

---

## Inspect Consumer Group Offsets and Lag

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

---

## Inspect Consumer Group Members and Partition Assignments

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

---

## Delete and Recreate the Course Events Topic

Delete:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --delete `
    --topic course-events
```

Recreate using `kafka-init` from `compose.yaml`:

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

---

## Run a Second Enrollment Instance

From the repository root:

```powershell
cd services\enrollment-service
```

Start the second instance on port `8083`:

```powershell
.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.profiles=local" `
    "-Dspring-boot.run.arguments=--server.port=8083"
```

Both Enrollment instances use the same Kafka consumer group:

```text
enrollment-service
```

---

## Check Assignments with Two Enrollment Consumers

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

With 1 partition and 2 consumers, expect one consumer with `#PARTITIONS = 1` and one with `#PARTITIONS = 0`.

With 2 partitions and 2 consumers, expect both consumers to have a partition assignment.

---

## Stop an Enrollment Consumer

If the second instance is running in PowerShell:

```text
Ctrl+C
```

Then inspect the group again:

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

---

## Run Enrollment Tests

```powershell
cd services\enrollment-service
.\mvnw.cmd test
```

---

## Run Root Course Application Tests

From the repository root:

```powershell
.\mvnw.cmd test
```

---

# EVENT CONTRACT / SCHEMA EVOLUTION TESTS

## Publish an Old V1 Event

Use this to verify that the V2 consumer can still read an older event without `sourceService`.

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Paste:

```json
{"eventId":"11111111-1111-1111-1111-111111111111","eventType":"COURSE_CREATED","eventVersion":1,"occurredAt":"2026-09-16T08:00:00Z","courseId":9001,"name":"Kafka Contract V1 Replay","price":170.0,"difficulty":"MEDIUM"}
```

Expected consumer result:

```text
eventVersion=1
sourceService=null
```

---

## Publish a Breaking Contract Event

Use this to trigger a deserialization failure because `price` is sent as a String instead of a number.

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Paste:

```json
{"eventId":"22222222-2222-2222-2222-222222222222","eventType":"COURSE_CREATED","eventVersion":3,"sourceService":"course-service","occurredAt":"2026-09-16T09:00:00Z","courseId":9002,"name":"Kafka Broken Contract","price":"ONE HUNDRED SEVENTY EUROS","difficulty":"MEDIUM"}
```

---

# DEAD LETTER TOPIC

## Consume Records from the DLT

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

---

## Verify Processing Continues After a Poison Pill

```powershell
$body = @{
    name = "Kafka After Poison Pill"
    price = 180.0
    difficulty = "MEDIUM"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/jpa/courses" `
    -ContentType "application/json" `
    -Body $body
```

---

# RETRY TEST

## Publish a Valid Event That Triggers the Learning Retry Block

Enable the temporary `"Kafka Retry Test"` failure block in `CourseEventConsumer` first.

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

Then inspect the DLT:

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

---

# IDEMPOTENCY TEST

## Publish the Exact Same Event Twice

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Paste this event twice:

```json
{"eventId":"33333333-3333-3333-3333-333333333333","eventType":"COURSE_CREATED","eventVersion":2,"sourceService":"course-service","occurredAt":"2026-09-16T13:00:00Z","courseId":9003,"name":"Kafka Idempotency Test","price":175.0,"difficulty":"MEDIUM"}
```

Expected:

```text
first delivery  -> processed
second delivery -> Duplicate Kafka event skipped
```

---

## Inspect Processed Kafka Events in MongoDB

Open MongoDB shell:

```powershell
docker exec -it mongo mongosh
```

Then:

```javascript
use enrollment_management
```

```javascript
db.processed_kafka_events.find().pretty()
```

Find the idempotency test event:

```javascript
db.processed_kafka_events.find({
    _id: UUID("33333333-3333-3333-3333-333333333333")
}).pretty()
```
