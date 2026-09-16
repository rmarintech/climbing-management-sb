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

## Publish a Kafka Record Manually

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events
```

```json
{"eventType":"COURSE_CREATED","courseId":1,"name":"Sport Climbing","difficulty":"EASY"}
```

## Consume Records Manually

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:18082 `
    --topic course-events `
    --from-beginning
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

## Inspect Consumer Group

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```

## Delete the Topic

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --delete `
    --topic course-events
```

```powershell
docker compose run --rm kafka-init
```

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --topic course-events
```

## Run a Second Enrollment Instance

```powershell
cd services\enrollment-service
.\mvnw.cmd spring-boot:run `
  "-Dspring-boot.run.profiles=local" `
  "-Dspring-boot.run.arguments=--server.port=8083"
```

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:18082 `
    --describe `
    --group enrollment-service
```
