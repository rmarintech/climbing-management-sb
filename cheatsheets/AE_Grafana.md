GRAFANA COMMANDS — CLIMBING MANAGEMENT
======================================

1. START GRAFANA
   ================

# Create persistent storage.
docker volume create grafana-storage

# Start Grafana.
docker run `
  --name climbing-grafana `
-d `
  -p 3000:3000 `
-v grafana-storage:/var/lib/grafana `
grafana/grafana:13.2.2

Grafana UI:
http://localhost:3000

Default credentials:
username: admin
password: admin


2. CONNECT GRAFANA TO PROMETHEUS
   ================================

In Grafana:
Connections
→ Data sources
→ Add new data source
→ Prometheus

Prometheus URL:
http://host.docker.internal:9090

IMPORTANT:
Do NOT use:
http://localhost:9090

Inside the Grafana container, localhost means the Grafana container itself.

Current flow:
Enrollment Service
→ /actuator/prometheus
→ Prometheus
→ PromQL
→ Grafana
→ Dashboard


3. CREATE A DASHBOARD
   =====================

In Grafana:
Dashboards
→ New
→ New dashboard
→ Add visualization
→ Select Prometheus

For local testing use:
Last 15 minutes


4. HTTP OVERVIEW ROW
   ====================

Row name:
Enrollment Service — HTTP Overview


5. ENROLLMENT TRAFFIC
   =====================

Title:
Enrollment Traffic

Visualization:
Time series

PromQL:
sum(
rate(
http_server_requests_seconds_count{
uri="/enrollments"
}[1m]
)
)

Unit:
Requests/sec

Min:
0

Meaning:
Shows current request rate for /enrollments.

Example:
0.20 req/s ≈ 12 requests/minute


6. ENROLLMENT 5xx ERROR RATE
   ============================

Title:
Enrollment 5xx Error Rate

Visualization:
Time series

PromQL:
100 *
sum(
rate(
http_server_requests_seconds_count{
uri="/enrollments",
status=~"5.."
}[1m]
)
)
/
sum(
rate(
http_server_requests_seconds_count{
uri="/enrollments"
}[1m]
)
)

Unit:
Percent (0-100)

Min:
0

Max:
100

Threshold mode:
Absolute

Suggested thresholds:
Base → green
5 → warning
10 → critical

Meaning:
0% → no server errors
5% → 5% of recent requests returned 5xx
100% → all recent requests returned 5xx


7. ENROLLMENT GET p50 LATENCY
   =============================

Title:
Enrollment GET p50 Latency

Visualization:
Time series

PromQL:
histogram_quantile(
0.50,
sum by (le) (
rate(
http_server_requests_seconds_bucket{
uri="/enrollments",
method="GET",
status="200"
}[5m]
)
)
)

Unit:
seconds (s)

Min:
0

Meaning:
50% of successful GET requests completed within this duration.


8. ENROLLMENT GET p95 LATENCY
   =============================

Title:
Enrollment GET p95 Latency

PromQL:
histogram_quantile(
0.95,
sum by (le) (
rate(
http_server_requests_seconds_bucket{
uri="/enrollments",
method="GET",
status="200"
}[5m]
)
)
)

Unit:
seconds (s)

Meaning:
95% of successful GET requests completed within this duration.


9. ENROLLMENT GET p99 LATENCY
   =============================

Title:
Enrollment GET p99 Latency

PromQL:
histogram_quantile(
0.99,
sum by (le) (
rate(
http_server_requests_seconds_bucket{
uri="/enrollments",
method="GET",
status="200"
}[5m]
)
)
)

Unit:
seconds (s)

Meaning:
99% of successful GET requests completed within this duration.


10. LATENCY INTERPRETATION
    ==========================

Current learning example:
p50 ≈ 22 ms
p90 ≈ 81 ms
p95 ≈ 85 ms
p99 ≈ 89 ms

Meaning:
50% requests <= ~22 ms
90% requests <= ~81 ms
95% requests <= ~85 ms
99% requests <= ~89 ms

Typical ordering:
p50 <= p90 <= p95 <= p99

With very low traffic, percentiles can change a lot.


11. RUNTIME / SATURATION ROW
    ============================

Row name:
Enrollment Service — Runtime / Saturation


12. JVM CPU
    ===========

Title:
Enrollment JVM CPU

Visualization:
Time series

PromQL:
process_cpu_usage{
job="enrollment-service"
}

Unit:
Percent (0.0-1.0)

Min:
0

Example:
0.25 = 25% CPU

Current local workload:
~0.1% – 0.4%


13. JVM HEAP MEMORY
    ===================

Title:
Enrollment JVM Heap Memory

Visualization:
Time series

Unit:
Bytes (IEC)

Min:
0

Query A — Used Heap:
sum(
jvm_memory_used_bytes{
job="enrollment-service",
area="heap"
}
)

Legend:
Used Heap

Query B — Committed Heap:
sum(
jvm_memory_committed_bytes{
job="enrollment-service",
area="heap"
}
)

Legend:
Committed Heap

Meaning:
Used Heap → memory currently occupied by Java objects
Committed Heap → memory obtained by the JVM and available for heap usage

Typical healthy pattern:
allocation
→ heap rises
→ GC
→ heap drops
→ allocation continues


14. JVM HEAP UTILIZATION
    ========================

Title:
Enrollment JVM Heap Utilization

Visualization:
Time series

PromQL:
100 *
sum(
jvm_memory_used_bytes{
job="enrollment-service",
area="heap"
}
)
/
sum(
jvm_memory_max_bytes{
job="enrollment-service",
area="heap"
}
)

Unit:
Percent (0-100)

Min:
0

Max:
100

Formula:
Used Heap / Max Heap × 100

IMPORTANT:
Used / Max is different from Used / Committed.


15. JVM GC PAUSE
    ===============

Title:
Enrollment JVM GC Pause

Visualization:
Time series

PromQL:
sum(
rate(
jvm_gc_pause_seconds_sum{
job="enrollment-service"
}[5m]
)
)
/
sum(
rate(
jvm_gc_pause_seconds_count{
job="enrollment-service"
}[5m]
)
)

Unit:
seconds (s)

Min:
0

Meaning:
Average JVM garbage-collection pause duration.

Example:
0.003 s = 3 ms

Current local observation:
~1.5–3 ms


16. RAW GC METRICS
    ==================

jvm_gc_pause_seconds_count{
job="enrollment-service"
}

Possible labels:
action
cause
gc


17. JVM OBSERVABILITY RELATIONSHIP
    ==================================

Object allocations
→ Used Heap increases
→ Garbage Collector runs
→ GC Pause
→ Unused objects reclaimed
→ Used Heap decreases

Useful correlation:
Heap usage + GC pauses + CPU

Potential problematic pattern:
frequent GC
+ long pauses
+ high CPU
+ post-GC heap baseline continuously rising


18. GOLDEN SIGNALS DASHBOARD
    ============================

LATENCY:
p50
p95
p99

TRAFFIC:
requests/sec

ERRORS:
5xx error rate

SATURATION:
CPU
Heap Memory
Heap Utilization
GC Pause


19. CURRENT DASHBOARD STRUCTURE
    ===============================

Enrollment Service — HTTP Overview
├── Enrollment Traffic
├── Enrollment 5xx Error Rate
├── Enrollment GET p50 Latency
├── Enrollment GET p95 Latency
└── Enrollment GET p99 Latency

Enrollment Service — Runtime / Saturation
├── Enrollment JVM CPU
├── Enrollment JVM Heap Memory
├── Enrollment JVM Heap Utilization
└── Enrollment JVM GC Pause


20. KAFKA OBSERVABILITY
    =======================

Row:
Enrollment Service — Dependencies / Messaging

Current panels:
├── Kafka Lag by Partition
├── Kafka Total Consumer Lag
├── Kafka Assigned Partitions
├── Kafka Consumer Throughput
├── Kafka Listener Processing Time
└── Kafka Listener Failure Rate

21. KAFKA LAG BY PARTITION
    ==========================

Title: Kafka Lag by Partition
Visualization: Time series

PromQL:
sum by (topic, partition) (
kafka_consumer_fetch_manager_records_lag{
job="enrollment-service"
}
)

Legend:
{{topic}} / partition {{partition}}

Unit: short
Min: 0

lag = 0 → consumer is caught up
lag > 0 → records are waiting

Current local result:
partition 0 → 0
partition 1 → 0

22. KAFKA TOTAL CONSUMER LAG
    ============================

Title: Kafka Total Consumer Lag
Visualization: Stat

PromQL:
sum(
kafka_consumer_fetch_manager_records_lag{
job="enrollment-service"
}
)

Legend: Total Lag
Unit: short
Min: 0

Current healthy local result:
0

23. KAFKA ASSIGNED PARTITIONS
    =============================

Title: Kafka Assigned Partitions
Visualization: Stat

PromQL:
kafka_consumer_coordinator_assigned_partitions{
job="enrollment-service"
}

Legend: Assigned Partitions
Unit: short
Min: 0

Current local result:
2

Meaning:
The current Enrollment consumer owns both course-events partitions.

24. KAFKA CONSUMER THROUGHPUT
    =============================

Title: Kafka Consumer Throughput
Visualization: Time series

PromQL:
sum(
rate(
kafka_consumer_fetch_manager_records_consumed_total{
job="enrollment-service"
}[1m]
)
)

Legend: Records/sec
Unit: records/sec
Min: 0

Useful correlation:
throughput > 0 + lag = 0 → keeping up
throughput > 0 + lag rising → falling behind
throughput = 0 + lag > 0 → potentially stopped / blocked

25. KAFKA LISTENER PROCESSING TIME
    ==================================

Title: Kafka Listener Processing Time
Visualization: Time series

PromQL:
sum(
rate(
spring_kafka_listener_seconds_sum{
job="enrollment-service"
}[5m]
)
)
/
sum(
rate(
spring_kafka_listener_seconds_count{
job="enrollment-service"
}[5m]
)
)

Legend: Avg Listener Time
Unit: seconds (s)
Min: 0

26. KAFKA LISTENER FAILURE RATE
    ===============================

Title: Kafka Listener Failure Rate
Visualization: Time series

PromQL:
100 *
sum(
rate(
spring_kafka_listener_seconds_count{
job="enrollment-service",
result="failure"
}[5m]
)
)
/
sum(
rate(
spring_kafka_listener_seconds_count{
job="enrollment-service"
}[5m]
)
)

Legend: Listener Failure %
Unit: Percent (0-100)
Min: 0
Max: 100

Raw inspection:
spring_kafka_listener_seconds_count{
job="enrollment-service"
}

Useful labels:
result
exception
name

Important:
A result="failure" series may not exist until a listener failure occurs.

27. KAFKA FAILURE / RETRY / DLT EXPERIMENT
    ===========================================

The intentional "Kafka Retry Test" event was reused to verify observability.

Observed:
Course event
→ listener failure
→ retries
→ result="failure" metric
→ Grafana failure-rate spike
→ retries exhausted
→ course-events-dlt

After the test, disable the artificial failure condition again.

28. CURRENT DASHBOARD STRUCTURE
    ===============================

Enrollment Service — HTTP Overview
├── Enrollment Traffic
├── Enrollment 5xx Error Rate
├── Enrollment GET p50 Latency
├── Enrollment GET p95 Latency
└── Enrollment GET p99 Latency

Enrollment Service — Runtime / Saturation
├── Enrollment JVM CPU
├── Enrollment JVM Heap Memory
├── Enrollment JVM Heap Utilization
└── Enrollment JVM GC Pause

Enrollment Service — Dependencies / Messaging
├── Kafka Lag by Partition
├── Kafka Total Consumer Lag
├── Kafka Assigned Partitions
├── Kafka Consumer Throughput
├── Kafka Listener Processing Time
└── Kafka Listener Failure Rate

29. PANEL LEGENDS
    =================

Enrollment Traffic → Requests/sec
Enrollment 5xx Error Rate → 5xx %
Enrollment GET p50 Latency → p50
Enrollment GET p95 Latency → p95
Enrollment GET p99 Latency → p99
Enrollment JVM CPU → CPU
Enrollment JVM Heap Memory A → Used Heap
Enrollment JVM Heap Memory B → Committed Heap
Enrollment JVM Heap Utilization → Heap Utilization
Enrollment JVM GC Pause → Avg GC Pause
Kafka Lag by Partition → {{topic}} / partition {{partition}}
Kafka Total Consumer Lag → Total Lag
Kafka Assigned Partitions → Assigned Partitions
Kafka Consumer Throughput → Records/sec
Kafka Listener Processing Time → Avg Listener Time
Kafka Listener Failure Rate → Listener Failure %

30. NEXT — CUSTOM MICROMETER METRICS
    ====================================

Framework metrics now cover:
HTTP
JVM
MongoDB
Kafka client
Spring Kafka listener

Next:
Custom application / business metrics

First planned metric:
Dedicated DLT event counter

Reason:
Generic listener metrics can show failures, but they do not directly answer:
"How many records were sent to course-events-dlt?"
