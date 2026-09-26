LOKI / ALLOY COMMANDS — CLIMBING MANAGEMENT
============================================

1. ENROLLMENT STRUCTURED LOGGING
   ================================

# application.properties
logging.structured.format.console=logstash
logging.file.name=logs/enrollment-service.log
logging.structured.format.file=logstash

# .gitignore
logs/


2. ARCHITECTURE
   ===============

Enrollment Service
↓
JSON log file
↓
Alloy
↓
Loki
↓
Grafana

Metrics → Prometheus → Grafana
Traces  → OpenTelemetry → Tempo → Grafana
Logs    → Alloy → Loki → Grafana


3. LOKI
   =======

cd C:\Users\ruben\IdeaProjects\climbing-management-sb
$lokiConfig = (Resolve-Path .\observability\loki\loki-config.yaml).Path

docker volume create loki-data

docker run --rm `
  -v loki-data:/loki `
alpine `
sh -c "chown -R 10001:10001 /loki"

docker run `
  --name climbing-loki `
-d `
  -p 3100:3100 `
--mount "type=bind,source=$lokiConfig,target=/etc/loki/loki-config.yaml,readonly" `
  --mount "type=volume,source=loki-data,target=/loki" `
grafana/loki:3.7.0 `
"-config.file=/etc/loki/loki-config.yaml"

curl.exe http://localhost:3100/ready


4. ALLOY
   ========

$alloyConfig = (Resolve-Path .\observability\alloy\config.alloy).Path
$logDir = (Resolve-Path .\services\enrollment-service\logs).Path

docker volume create alloy-data

docker run `
  --name climbing-alloy `
-d `
  -p 12345:12345 `
--mount "type=bind,source=$alloyConfig,target=/etc/alloy/config.alloy,readonly" `
  --mount "type=bind,source=$logDir,target=/var/log/climbing,readonly" `
--mount "type=volume,source=alloy-data,target=/var/lib/alloy/data" `
  grafana/alloy:latest `
run `
  --server.http.listen-addr=0.0.0.0:12345 `
--storage.path=/var/lib/alloy/data `
/etc/alloy/config.alloy

# Verify
Invoke-RestMethod http://localhost:3100/loki/api/v1/labels
Invoke-RestMethod http://localhost:3100/loki/api/v1/label/job/values


5. GRAFANA → LOKI
   =================

Connections → Data sources → Add data source → Loki

Name:
Loki

URL:
http://host.docker.internal:3100


6. LOGQL
   ========

{job="enrollment-service"}

{job="enrollment-service"} | json

{job="enrollment-service"} | json | level="INFO"

{job="enrollment-service"} | json | eventType="COURSE_CREATED"

{job="enrollment-service"} | json | courseId="82"

{job="enrollment-service"} | json | traceId="..."


7. STRUCTURED LOGGING
   =====================

log.atInfo()
.addKeyValue("eventId", event.eventId())
.addKeyValue("eventType", event.eventType())
.addKeyValue("courseId", event.courseId())
.addKeyValue("partition", record.partition())
.addKeyValue("offset", record.offset())
.log("CourseCreatedEvent consumed");

Result:

{
"message": "CourseCreatedEvent consumed",
"courseId": 82,
"partition": 0,
"traceId": "...",
"spanId": "..."
}


8. CARDINALITY
   ==============

Good Loki labels:
job
service

Do NOT use as stream labels:
traceId
spanId
eventId
courseId

Low cardinality → labels / metrics
High cardinality → logs / traces


9. LOGS → TRACES
   ================

Loki data source
→ Additional settings
→ Derived fields

Name:
TraceID

Regex:
"traceId":"(\w+)"

Internal link:
ON

Data source:
Tempo

Query:
${__value.raw}

Flow:

Loki log
↓
traceId
↓
Tempo trace


10. TRACES → LOGS
    =================

Tempo data source
→ Trace to logs

Data source:
Loki

Tag mapping:
service.name → service

Filter by trace ID:
ON

Filter by span ID:
OFF

Optional:
start shift = -2s
end shift   = 2s

Flow:

Tempo span
↓
service + traceId
↓
Loki logs


11. BIDIRECTIONAL CORRELATION
    =============================

Loki ── traceId ──► Tempo
▲                  │
└──── logs ────────┘

Logs → Traces ✅
Traces → Logs ✅

12. CURRENT STATUS
    =================

Structured JSON logs              ✅
SLF4J key/value business fields   ✅
Alloy file collection             ✅
Loki ingestion                    ✅
LogQL JSON queries                ✅
Loki → Tempo                      ✅
Tempo → Loki                      ✅

High-cardinality identifiers stay in log fields:
traceId
spanId
eventId
courseId

Low-cardinality stream labels:
job
service
