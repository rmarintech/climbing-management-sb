TEMPO COMMANDS — CLIMBING MANAGEMENT
====================================

1. TEMPO — LOCAL DOCKER SETUP
   ==============================

# Run from project root:
# C:\Users\ruben\IdeaProjects\climbing-management-sb

# Resolve Tempo config path.
$tempoConfig = (Resolve-Path .\observability\tempo\tempo.yaml).Path

# Create persistent storage.
docker volume create tempo-data

# Tempo runs as non-root UID 10001.
docker run --rm `
  -v tempo-data:/data/tempo `
alpine `
sh -c "chown -R 10001:10001 /data/tempo"

# Start Tempo.
# 3200 → Tempo HTTP API
# 4317 → OTLP gRPC
# 4318 → OTLP HTTP
docker run `
  --name climbing-tempo `
-d `
  -p 3200:3200 `
-p 4317:4317 `
  -p 4318:4318 `
--mount "type=bind,source=$tempoConfig,target=/etc/tempo.yaml,readonly" `
  --mount "type=volume,source=tempo-data,target=/data/tempo" `
grafana/tempo:3.0.3 `
"-config.file=/etc/tempo.yaml"

# Verify Tempo.
curl.exe http://localhost:3200/ready

# Logs.
docker logs climbing-tempo
docker logs -f climbing-tempo

# Stop / start / remove.
docker stop climbing-tempo
docker start climbing-tempo
docker rm -f climbing-tempo


2. BASIC TEMPO SEARCH
   =====================

# Check whether Tempo contains traces.
http://localhost:3200/api/search?q=%7B%7D

# Known service.name values.
http://localhost:3200/api/search/tag/service.name/values

# Search enrollment-service.
http://localhost:3200/api/search?q=%7B%20resource.service.name%20%3D%20%22enrollment-service%22%20%7D


3. POWERSHELL TRACEQL SEARCH
   ===========================

# enrollment-service traces containing "enrollments".
$q = '{ resource.service.name = "enrollment-service" && span:name =~ ".*enrollments.*" }'
$url = "http://localhost:3200/api/search?q=" + [uri]::EscapeDataString($q)
$r = Invoke-RestMethod $url
$r.traces |
Select-Object traceID, rootServiceName, rootTraceName, durationMs

# Recent enrollment-service traces.
$q = '{ resource.service.name = "enrollment-service" }'
$url = "http://localhost:3200/api/search?q=" + [uri]::EscapeDataString($q) + "&limit=100"
$r = Invoke-RestMethod $url
$r.traces |
Select-Object traceID, rootServiceName, rootTraceName, durationMs


4. SEARCH A KNOWN TRACE
   =======================

# Retrieve a trace by ID.
http://localhost:3200/api/traces/7792bc12477c83e16e8bebe81c2aa2de


5. SEARCH MOST RECENT HTTP TRACE
   ================================

# Most recent POST /enrollments.
$q = '{ trace:rootService = "enrollment-service" && trace:rootName = "http post /enrollments" } with (most_recent=true)'
$url = "http://localhost:3200/api/search?q=" + [uri]::EscapeDataString($q)
$r = Invoke-RestMethod $url
$r.traces |
Select-Object traceID, rootServiceName, rootTraceName, durationMs

# Most recent POST /jpa/courses.
$q = '{ trace:rootService = "course-service" && trace:rootName = "http post /jpa/courses" } with (most_recent=true)'
$url = "http://localhost:3200/api/search?q=" + [uri]::EscapeDataString($q)
$r = Invoke-RestMethod $url
$r.traces |
Select-Object traceID, rootServiceName, rootTraceName, durationMs


6. HISTORICAL SEARCH WINDOW
   ===========================

# Search last hour.
$end = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$start = $end - 3600

$q = '{ resource.service.name = "enrollment-service" }'
$url = "http://localhost:3200/api/search?q=" +
[uri]::EscapeDataString($q) +
"&start=$start&end=$end&limit=100"

$r = Invoke-RestMethod $url
$r.traces |
Select-Object traceID, rootServiceName, rootTraceName, durationMs


7. GRAFANA → TEMPO
   ==================

Grafana
↓
Connections
↓
Data sources
↓
Add data source
↓
Tempo

Name:
Tempo

URL:
http://host.docker.internal:3200

# Why not localhost?
# Grafana runs inside Docker.
# localhost → Grafana container itself
# host.docker.internal → Windows host → Tempo:3200


8. OBSERVABILITY ARCHITECTURE
   =============================

Metrics:

Application
↓
Micrometer
↓
/actuator/prometheus
↓
Prometheus
↓
Grafana

Traces:

Application
↓
Micrometer Observation / Tracing
↓
OpenTelemetry
↓
OTLP HTTP
↓
Tempo
↓
Grafana


9. OPENTELEMETRY CONFIGURATION
   ==============================

management.tracing.sampling.probability=1.0
management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
management.opentelemetry.tracing.export.otlp.transport=http

# Prometheus handles metrics.
# Tempo receives traces only.
management.otlp.metrics.export.enabled=false


10. SERVICE NAMES
    =================

# Course Service
spring.application.name=course-service

# Enrollment Service
spring.application.name=enrollment-service

# service.name becomes visible in Tempo / OpenTelemetry.


11. HTTP DISTRIBUTED TRACING
    ============================

Enrollment Service
↓
HTTP CLIENT span
↓
traceparent HTTP header
↓
Course Service
↓
HTTP SERVER span

Example:

enrollment-service
└── POST /enrollments
└── HTTP GET
└── course-service
└── GET /jpa/courses/{id}

Important:
- Same distributed operation → same traceId
- Each span → different spanId


12. KAFKA DISTRIBUTED TRACING
    =============================

Course Service
↓
Kafka producer
↓
course-events
↓
Kafka consumer
↓
Enrollment Service

Trace context is propagated through Kafka record headers.

Producer:
course-service
└── course-events send

Consumer:
enrollment-service
└── course-events process

Both belong to the same distributed trace.


13. COURSE SERVICE — KAFKA PRODUCER TRACING
    ===========================================

# Course Service is the Kafka producer.
spring.kafka.template.observation-enabled=true

HTTP request
↓
course-service
↓
KafkaTemplate
↓
course-events send span
↓
trace context injected into Kafka headers


14. ENROLLMENT SERVICE — KAFKA CONSUMER TRACING
    ================================================

# Enrollment Service is the Kafka consumer.
spring.kafka.listener.observation-enabled=true

Kafka record
↓
trace context extracted
↓
course-events process span
↓
@KafkaListener


15. KAFKA OBSERVATION VS OLD MICROMETER TIMERS
    ==============================================

Before:

Kafka Micrometer Timer
↓
metrics only

After:

Kafka Observation
↓
metrics
+
traces
+
context propagation

When observationEnabled=true, Spring Kafka uses Observation instrumentation.

Existing Kafka timer metrics / labels may change.
Grafana Kafka panels should be checked after enabling Observation.


16. ASYNCHRONOUS TRACE TIMING
    =============================

Kafka traces can contain gaps:

course-events send
│
│ no active instrumented span
▼
course-events process

The gap can contain:
- Kafka broker storage
- record waiting in partition
- consumer polling
- consumer thread scheduling
- deserialization / framework processing
- consumer observation startup

Important:
Do not interpret the whole gap as "Kafka broker latency".

Tracing only tells us that no instrumented span covers that period.


17. TRACE VS REQUEST
    ====================

A distributed trace can live longer than the original HTTP request.

Example:

0s
│
├── POST /jpa/courses
├── Kafka SEND
│
1.7s
│   HTTP request finished
│
│   asynchronous Kafka processing
│
3.5s
│
├── Enrollment Kafka consumer starts
│
4.7s
│
└── Distributed trace finishes


18. TRACE / LOG CORRELATION
    ===========================

Micrometer Tracing adds:
- traceId
- spanId

Course producer log:

traceId =
55d9f80db7338a3e41de62143ddfd239

spanId =
abfd304c1d02a229

Enrollment consumer log:

traceId =
55d9f80db7338a3e41de62143ddfd239

spanId =
b6ba9bf37af32aa1

Same:
traceId

Different:
spanId

Meaning:

Course producer
↓
same distributed trace
↓
Enrollment consumer

This allows:

Log
↓
copy traceId
↓
Tempo
↓
inspect entire distributed operation


19. THREE OBSERVABILITY SIGNALS
    ===============================

METRICS
Prometheus
↓
What is happening?

TRACES
Tempo
↓
Where is it happening?

LOGS
traceId / spanId
↓
What exactly happened?

Together:

Metrics
↓
detect problem

Trace
↓
locate problem

Logs
↓
understand details