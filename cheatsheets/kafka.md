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

## List Kafka Topics

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --list
```

## Describe the Course Events Topic

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

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

## Publish a Kafka Record Manually

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

Paste an event:

```json
{"eventType":"COURSE_CREATED","courseId":1,"name":"Sport Climbing","difficulty":"EASY"}
```

Exit:

```text
Ctrl+C
```

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

## Inspect Consumer Group Offsets and Lag

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

## Inspect Consumer Group Members and Partition Assignments

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

## Delete and Recreate the Topic

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

## Stop an Enrollment Consumer

If the second instance is running in PowerShell:

```text
Ctrl+C
```

Then inspect the group again to observe reassignment:

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service `
    --members `
    --verbose
```

## Run Enrollment Tests

```powershell
cd services\enrollment-service
.\mvnw.cmd test
```
