# Observability

This document contains the observability theory, configuration, PromQL examples and Grafana notes used in the **Climbing Management** learning project.

Project progress remains tracked only in [ROADMAP.md](ROADMAP.md).

---

# 1. Why Observability Matters

Observability is the ability to understand the internal state of a system from the signals it exposes externally.

In a distributed backend, the important question is not only:

```text
Is the application running?
```

but also:

```text
Why is it slow?
Why are requests failing?
Which dependency is causing the delay?
Is the consumer falling behind?
Is the JVM under memory pressure?
```

The three classic observability signals are:

```text
Logs
Metrics
Traces
```

They answer different questions:

```text
Logs
→ What happened?

Metrics
→ How much / how often / how fast?

Traces
→ Where did the request spend its time?
```

They are complementary rather than interchangeable.

---

# 2. Instrumentation, Telemetry, Monitoring and Observability

```text
Application
    ↓
Instrumentation
    ↓
Telemetry
    ↓
Collection / storage
    ↓
Monitoring / dashboards
    ↓
Observability
```

## Instrumentation

Code or framework integration that records measurements.

In this project, Spring Boot and Micrometer automatically instrument many areas such as:

```text
HTTP
JVM
process
MongoDB
Kafka
Spring Security
Tomcat
executors
```

## Telemetry

The data produced by instrumentation:

```text
metrics
logs
traces
```

## Monitoring

Watching known signals and conditions.

Example:

```text
5xx error rate > 10%
```

## Observability

Using telemetry to investigate both known and previously unknown system behavior.

---

# 3. Golden Signals

A useful service dashboard usually starts with the four Golden Signals:

```text
Latency
Traffic
Errors
Saturation
```

For the Enrollment Service:

```text
Latency
→ HTTP average / p50 / p95 / p99

Traffic
→ requests per second

Errors
→ HTTP 5xx rate

Saturation
→ CPU / heap / GC / Kafka lag
```

---

# 4. Micrometer and Spring Boot Actuator

Spring Boot Actuator exposes operational information from the application.

Micrometer provides the metrics abstraction used by Spring Boot.

Conceptually:

```text
Spring application
    ↓
Micrometer instruments components
    ↓
MeterRegistry
    ↓
Actuator
    ↓
metrics exported to monitoring backend
```

Useful local endpoints:

```text
http://localhost:8081/actuator/health
http://localhost:8081/actuator/metrics
http://localhost:8081/actuator/prometheus
```

Current Enrollment Service exposure:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

Prometheus registry dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

The local learning setup allows Prometheus to scrape:

```text
/actuator/prometheus
```

without a human Keycloak token.

This is a local development decision. In production, management endpoints should normally be protected using infrastructure controls such as a private management network, dedicated management port, firewall rules, Kubernetes NetworkPolicies, service authentication or mTLS rather than being publicly exposed.

---

# 5. Metrics Model

A Prometheus time series is identified by:

```text
metric name
+
label combination
=
unique time series
```

Example:

```text
http_server_requests_seconds_count{
  method="GET",
  status="200",
  uri="/enrollments"
}
```

and:

```text
http_server_requests_seconds_count{
  method="POST",
  status="503",
  uri="/enrollments"
}
```

are different time series.

Typical Spring / Micrometer labels include:

```text
method
status
uri
outcome
error
exception
```

Prometheus adds scrape-target labels such as:

```text
job
instance
```

Example:

```text
job="enrollment-service"
instance="host.docker.internal:8081"
```

---

# 6. Cardinality

Every unique label combination creates another time series.

Low-cardinality labels are normally appropriate:

```text
method=GET
status=200
uri=/enrollments
```

High-cardinality identifiers are dangerous as metric labels:

```text
userId
enrollmentId
traceId
requestId
```

These can create huge numbers of time series.

A useful rule:

```text
Metrics
→ low-cardinality dimensions

Logs / traces
→ high-cardinality identifiers
```

---

# 7. Metric Types

## Counter

A value that normally only increases.

Example:

```text
http_server_requests_seconds_count
```

Use cases:

```text
request count
error count
event count
```

Typical PromQL:

```promql
rate(counter[5m])
```

or:

```promql
increase(counter[5m])
```

## Gauge

A value that can move up and down.

Examples:

```text
CPU usage
memory usage
thread count
connection pool size
```

## Timer

Micrometer timers measure duration and count.

For HTTP requests Prometheus exports metrics including:

```text
http_server_requests_seconds_count
http_server_requests_seconds_sum
http_server_requests_seconds_max
```

When histogram publishing is enabled:

```text
http_server_requests_seconds_bucket
```

is also available.

---

# 8. Prometheus

Prometheus collects and stores time-series metrics by scraping HTTP endpoints.

Current local flow:

```text
Enrollment Service
localhost:8081
    │
    │ /actuator/prometheus
    ▼
Prometheus container
localhost:9090
```

Prometheus configuration:

```text
observability/prometheus/prometheus.yml
```

Current learning configuration:

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: "enrollment-service"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets:
          - "host.docker.internal:8081"
```

Why `host.docker.internal`?

```text
Prometheus runs inside Docker.
Enrollment currently runs from IntelliJ on Windows.

localhost inside the Prometheus container
!=
Windows host localhost
```

Docker Desktop provides:

```text
host.docker.internal
```

so the container can reach a service running on the host.

Start Prometheus:

```powershell
docker run `
  --name climbing-prometheus `
  -p 9090:9090 `
  -v "${PWD}\observability\prometheus\prometheus.yml:/etc/prometheus/prometheus.yml:ro" `
  prom/prometheus:v3.14.0
```

Prometheus UI:

```text
http://localhost:9090
```

---

# 9. Verify the Prometheus Target

Query:

```promql
up{job="enrollment-service"}
```

Interpretation:

```text
1
→ target is being scraped successfully

0
→ target is known but scraping is failing
```

---

# 10. PromQL Basics

## Instant Vector

```promql
http_server_requests_seconds_count{
  uri="/enrollments"
}
```

Returns the latest sample for every matching time series.

## Range Vector

```promql
http_server_requests_seconds_count{
  uri="/enrollments"
}[1m]
```

Returns the samples from the last minute for every matching time series.

Functions such as:

```text
rate()
increase()
```

operate on range vectors.

---

# 11. HTTP Request Counter

All HTTP request counters:

```promql
http_server_requests_seconds_count
```

Only Enrollment requests:

```promql
http_server_requests_seconds_count{
  uri="/enrollments"
}
```

The counter is cumulative for the lifetime of the current application process.

A JVM restart resets in-memory Micrometer counters.

Prometheus, however, retains samples it has already scraped according to its own retention configuration.

---

# 12. Request Rate

```promql
rate(
  http_server_requests_seconds_count{
    uri="/enrollments"
  }[1m]
)
```

`rate(...)` estimates the average per-second increase of a counter during the selected range.

Example:

```text
0.05 requests / second
```

is roughly:

```text
0.05 × 60
≈ 3 requests / minute
```

Aggregate by method and status:

```promql
sum by (method, status) (
  rate(
    http_server_requests_seconds_count{
      uri="/enrollments"
    }[1m]
  )
)
```

---

# 13. Increase

```promql
sum by (method, status) (
  increase(
    http_server_requests_seconds_count{
      uri="/enrollments"
    }[1m]
  )
)
```

Conceptually:

```text
rate(...)
→ approximate increase per second

increase(...)
→ approximate total increase over the selected range
```

`increase()` can return fractional values because Prometheus estimates the counter change from scraped samples and extrapolates to the requested time boundaries.

It is not an authoritative business-event count.

---

# 14. Average HTTP Latency

For a timer:

```text
_sum
→ accumulated duration

_count
→ number of completed observations
```

Average latency:

```text
rate(_sum)
-----------
rate(_count)
```

PromQL:

```promql
rate(
  http_server_requests_seconds_sum{
    uri="/enrollments"
  }[1m]
)
/
rate(
  http_server_requests_seconds_count{
    uri="/enrollments"
  }[1m]
)
```

Successful GET only:

```promql
rate(
  http_server_requests_seconds_sum{
    uri="/enrollments",
    method="GET",
    status="200"
  }[1m]
)
/
rate(
  http_server_requests_seconds_count{
    uri="/enrollments",
    method="GET",
    status="200"
  }[1m]
)
```

The result is expressed in seconds.

Example:

```text
0.0107
≈ 10.7 ms
```

---

# 15. HTTP 5xx Error Rate

Service error rate:

```text
server errors
-------------
all requests
```

PromQL percentage:

```promql
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
```

`=~` means regular-expression match.

```text
status=~"5.."
```

matches:

```text
500
501
502
503
504
...
```

A 4xx response should normally be analyzed separately because authentication failures, authorization failures and invalid client requests are not the same reliability signal as server-side 5xx failures.

---

# 16. Why Average Latency Is Not Enough

Suppose most requests are fast but one is very slow.

```text
20 ms
21 ms
18 ms
19 ms
22 ms
1200 ms
```

The average is pulled upward but does not describe the distribution.

Percentiles provide a better view of the user experience:

```text
p50
→ median / typical request

p90
→ 90% completed within this duration

p95
→ 95% completed within this duration

p99
→ extreme tail
```

Typical ordering for the same dataset:

```text
p50 <= p90 <= p95 <= p99
```

---

# 17. HTTP Histogram

Enable Micrometer percentile histogram publishing:

```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

Prometheus then receives bucket metrics:

```text
http_server_requests_seconds_bucket
```

Example:

```text
le="0.005"   1
le="0.010"   3
le="0.025"   7
le="+Inf"    8
```

`le` means:

```text
less than or equal
```

Histogram buckets are cumulative:

```text
≤ 5 ms  → 1 request
≤ 10 ms → 3 requests total
≤ 25 ms → 7 requests total
+Inf     → 8 requests total
```

---

# 18. Inspect Enrollment Histogram Buckets

```promql
http_server_requests_seconds_bucket{
  uri="/enrollments",
  method="GET",
  status="200"
}
```

Each `le` value is a different bucket boundary.

The `+Inf` bucket represents all observations and should match the corresponding `_count`.

---

# 19. p50 / p90 / p95 / p99

## p50

```promql
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
```

## p90

```promql
histogram_quantile(
  0.90,
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
```

## p95

```promql
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
```

## p99

```promql
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
```

Read the query from inside to outside:

```text
http_server_requests_seconds_bucket
        ↓
select histogram buckets

rate(...[5m])
        ↓
bucket growth during the last five minutes

sum by (le)
        ↓
aggregate while preserving bucket boundaries

histogram_quantile(...)
        ↓
estimate percentile
```

`histogram_quantile()` estimates within buckets; it does not return an exact recorded request duration.

With very low local traffic, percentile graphs can move sharply as requests enter and leave the selected range.

---

# 20. Grafana

Grafana visualizes data queried from Prometheus.

Conceptually:

```text
Micrometer
→ instruments application

Actuator
→ exposes metrics

Prometheus
→ scrapes and stores metrics

PromQL
→ queries metrics

Grafana
→ visualizes PromQL results
```

Create persistent storage:

```powershell
docker volume create grafana-storage
```

Run Grafana:

```powershell
docker run `
  --name climbing-grafana `
  -d `
  -p 3000:3000 `
  -v grafana-storage:/var/lib/grafana `
  grafana/grafana:13.2.2
```

Grafana UI:

```text
http://localhost:3000
```

Current local Prometheus data-source URL from the Grafana container:

```text
http://host.docker.internal:9090
```

Using `localhost:9090` inside Grafana would point at the Grafana container itself rather than the Windows host.

---

# 21. Grafana HTTP Dashboard

Current row:

```text
Enrollment Service — HTTP Overview
```

Panels:

```text
Enrollment Traffic
Enrollment 5xx Error Rate
Enrollment GET p50 Latency
Enrollment GET p95 Latency
Enrollment GET p99 Latency
```

## Traffic

```promql
sum(
  rate(
    http_server_requests_seconds_count{
      uri="/enrollments"
    }[1m]
  )
)
```

Recommended unit:

```text
requests/sec
```

## 5xx Error Rate

```promql
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
```

Recommended unit:

```text
Percent (0-100)
```

Learning thresholds:

```text
Base      → healthy
5%        → warning
10%       → critical
```

Threshold mode:

```text
Absolute
```

These are learning/dashboard thresholds, not production SLOs.

## Latency Panels

Use the p50 / p95 / p99 queries from the histogram section.

Recommended unit:

```text
seconds
```

Grafana automatically displays small values as milliseconds.

---

# 22. Time Range

During local manual testing, a shorter dashboard time range is easier to read.

Example:

```text
Last 15 minutes
```

instead of:

```text
Last 6 hours
```

The dashboard time range controls what historical samples Grafana requests from Prometheus.

The PromQL range selector is a different concept:

```text
Dashboard range
→ how much history is displayed

[1m] / [5m] in PromQL
→ how much data each calculation uses at each evaluation point
```

---

# 23. JVM CPU

Panel:

```text
Enrollment JVM CPU
```

Query:

```promql
process_cpu_usage{
  job="enrollment-service"
}
```

The value is a ratio:

```text
0.00 → 0%
0.25 → 25%
1.00 → 100%
```

Recommended Grafana unit:

```text
Percent (0.0-1.0)
```

The current local workload is intentionally tiny, so very low process CPU is expected.

---

# 24. JVM Heap Memory

Panel:

```text
Enrollment JVM Heap Memory
```

Used heap:

```promql
sum(
  jvm_memory_used_bytes{
    job="enrollment-service",
    area="heap"
  }
)
```

Committed heap:

```promql
sum(
  jvm_memory_committed_bytes{
    job="enrollment-service",
    area="heap"
  }
)
```

Recommended unit:

```text
bytes (IEC)
```

Concepts:

```text
Used
→ memory currently occupied

Committed
→ memory obtained by the JVM and available for heap use

Max
→ upper limit the JVM can grow toward
```

---

# 25. JVM Heap Sawtooth

The current local heap graph shows the classic GC pattern:

```text
heap
  │       /|       /|
  │      / |      / |
  │     /  |     /  |
  │____/   |____/   |____
              ↑
              GC
```

Flow:

```text
objects allocated
    ↓
used heap rises
    ↓
garbage collector runs
    ↓
unreachable objects reclaimed
    ↓
used heap drops
```

A useful signal is the post-GC baseline.

Healthy-looking local behavior:

```text
GC repeatedly returns used memory to a similar baseline
```

A possible leak pattern would be:

```text
post-GC baseline keeps rising over time
```

A real leak diagnosis requires longer observation and additional evidence; heap shape alone is not proof.

---

# 26. JVM Heap Utilization

Panel:

```text
Enrollment JVM Heap Utilization
```

Query:

```promql
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
```

This measures:

```text
used heap
---------
max heap
× 100
```

This is different from:

```text
used / committed
```

The JVM can have relatively high use of currently committed memory while still using only a small fraction of its configured maximum heap.

---

# 27. GC Pause Duration

Panel:

```text
Enrollment JVM GC Pause
```

Average pause query:

```promql
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
```

This uses the same timer pattern as HTTP latency:

```text
_sum
----
_count

=
average duration
```

Inspect raw GC series:

```promql
jvm_gc_pause_seconds_count{
  job="enrollment-service"
}
```

Different GC actions / causes can produce multiple time series.

The aggregate dashboard panel intentionally combines them into one service-level average.

---

# 28. Correlating JVM Signals

The current runtime row is:

```text
Enrollment Service — Runtime / Saturation
├── Enrollment JVM CPU
├── Enrollment JVM Heap Memory
├── Enrollment JVM Heap Utilization
└── Enrollment JVM GC Pause
```

A potentially concerning JVM pattern could involve several signals together:

```text
high heap pressure
+
post-GC baseline rising
+
frequent / long GC pauses
+
high CPU
```

Observability is stronger when signals are correlated rather than interpreted independently.

---

# 29. Kafka Messaging Dashboard

The third Grafana row is now:

```text
Enrollment Service — Dependencies / Messaging
├── Kafka Lag by Partition
├── Kafka Total Consumer Lag
├── Kafka Assigned Partitions
├── Kafka Consumer Throughput
├── Kafka Listener Processing Time
└── Kafka Listener Failure Rate
```

This row answers:
```text
Lag                     → Is work accumulating?
Assigned partitions     → What partitions does this consumer own?
Throughput              → How quickly are records consumed?
Listener processing     → How long does application processing take?
Listener failure rate   → How often does listener execution fail?
```

---

# 30. Kafka Consumer Lag

Raw metric:
```promql
kafka_consumer_fetch_manager_records_lag{
  job="enrollment-service"
}
```

Concept:
```text
latest broker offset - consumer offset = consumer lag
```

Interpretation:
```text
lag = 0
→ consumer is caught up

lag > 0
→ records are waiting to be consumed

lag continuously increasing
→ producer may be outrunning the consumer,
  or the consumer may be slow / blocked
```

Lag by partition:
```promql
sum by (topic, partition) (
  kafka_consumer_fetch_manager_records_lag{
    job="enrollment-service"
  }
)
```

Legend:
```text
{{topic}} / partition {{partition}}
```

Current local result:
```text
course-events / partition 0 → 0
course-events / partition 1 → 0
```

Total lag:
```promql
sum(
  kafka_consumer_fetch_manager_records_lag{
    job="enrollment-service"
  }
)
```

Recommended panel:
```text
Title: Kafka Total Consumer Lag
Visualization: Stat
Legend: Total Lag
Min: 0
```

Current healthy local result:
```text
Total Lag = 0
```

Important limitation: these Kafka client metrics are exposed by the Enrollment JVM. If the whole service is down, Prometheus can no longer scrape its embedded consumer metrics. Production Kafka monitoring may therefore also use broker / consumer-group monitoring external to the application.

---

# 31. Kafka Assigned Partitions

Query:
```promql
kafka_consumer_coordinator_assigned_partitions{
  job="enrollment-service"
}
```

Recommended panel:
```text
Title: Kafka Assigned Partitions
Visualization: Stat
Legend: Assigned Partitions
Min: 0
```

Current local result:
```text
2
```

The running Enrollment consumer currently owns both `course-events` partitions. During a rebalance or when multiple consumers in the same group are running, ownership can move between instances.

---

# 32. Kafka Consumer Throughput

Raw cumulative metric:
```promql
kafka_consumer_fetch_manager_records_consumed_total{
  job="enrollment-service"
}
```

Dashboard rate:
```promql
sum(
  rate(
    kafka_consumer_fetch_manager_records_consumed_total{
      job="enrollment-service"
    }[1m]
  )
)
```

Recommended panel:
```text
Title: Kafka Consumer Throughput
Visualization: Time series
Unit: records/sec
Legend: Records/sec
Min: 0
```

Useful correlation:
```text
throughput > 0 + lag = 0
→ consumer is processing and keeping up

throughput > 0 + lag rising
→ consumer is processing but not fast enough

throughput = 0 + lag > 0
→ consumer may be stopped, blocked or unhealthy
```

The local experiment produced visible throughput spikes while total lag remained at `0`.

---

# 33. Kafka Listener Processing Time

Spring Kafka exposes listener execution timers.

Raw count:
```promql
spring_kafka_listener_seconds_count{
  job="enrollment-service"
}
```

Average listener processing duration:
```promql
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
```

Recommended panel:
```text
Title: Kafka Listener Processing Time
Visualization: Time series
Unit: seconds (s)
Legend: Avg Listener Time
Min: 0
```

This measures application-level listener execution rather than only Kafka client transport behavior. Current local observations were in the millisecond range, but the sample is too small for production conclusions.

---

# 34. Kafka Listener Failure Rate

The listener timer includes labels such as:
```text
name
result
exception
```

A healthy observed series used:
```text
result="success"
exception="none"
```

Raw inspection:
```promql
spring_kafka_listener_seconds_count{
  job="enrollment-service"
}
```

Failure rate:
```promql
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
```

Recommended panel:
```text
Title: Kafka Listener Failure Rate
Visualization: Time series
Unit: Percent (0-100)
Legend: Listener Failure %
Min: 0
Max: 100
```

Important:
```text
0% with recent listener executions
→ executions occurred and none failed

No data
→ a failure series may not exist yet,
  or there may be no usable recent samples
```

A deserialization failure can happen before the application listener method is invoked, so not every Kafka failure mode is represented identically by the listener timer.

---

# 35. Failure / Retry / DLT Observability Experiment

The existing Kafka retry exercise was reused as an observability experiment.

The special event:
```text
Kafka Retry Test
```

was configured to deliberately fail during Enrollment listener processing.

Observed flow:
```text
CourseCreatedEvent
        ↓
course-events
        ↓
Enrollment @KafkaListener
        ↓
simulated processing exception
        ↓
retry attempts
        ↓
listener failure metric appears
        ↓
retries exhausted
        ↓
course-events-dlt
```

This connected the earlier Kafka resilience work with the observability stack:
```text
runtime behavior
→ Micrometer / Spring Kafka timer
→ Prometheus
→ PromQL
→ Grafana
```

After the experiment, the artificial failure condition should be disabled again.

---

# 36. Automatic Metrics vs Custom Application Metrics

Most metrics introduced earlier are automatically provided by frameworks:

```text
HTTP metrics
JVM / process metrics
MongoDB metrics
Kafka client metrics
Spring Kafka listener metrics
```

They answer many technical questions, but not every application-specific question. Examples:

```text
How many records were sent to the DLT?
Why were they sent there?
How many duplicate Kafka events were skipped?
How many Enrollment creation attempts occurred?
How many Enrollments were actually persisted?
How many requests failed because the Course did not exist?
```

Those questions require explicit instrumentation.

---

# 37. Custom Counter Fundamentals

The first custom metrics use Micrometer `Counter`.

A counter represents a monotonically increasing event count during the lifetime of the current JVM:

```text
0 → 1 → 2 → 3 → ...
```

Counters are useful for discrete events such as DLT recoveries, duplicates skipped, creation attempts, successful creations and validation failures.

A JVM restart resets the in-memory counter, while Prometheus retains previously scraped historical samples according to its own retention.

For recent activity, query the counter with:

```promql
increase(counter[5m])
```

or:

```promql
rate(counter[5m])
```

For rare discrete business events, `increase(...)` is usually easier to read in a dashboard. Because Prometheus extrapolates to the exact boundaries of the selected range, `increase(...)` can return a fractional estimate such as `2.03` even though the application counter increments in whole events.

---

# 38. Custom DLT Counter

Micrometer metric:

```text
climbing.kafka.dlt.events
```

Prometheus exposition:

```text
climbing_kafka_dlt_events_total
```

The counter is incremented only after successful DLT recovery:

```text
listener failure
    ↓
retries exhausted or fatal failure
    ↓
DeadLetterPublishingRecoverer succeeds
    ↓
RetryListener.recovered(...)
    ↓
DLT counter +1
```

It is intentionally not incremented from `RetryListener.recoveryFailed(...)`, because a failed DLT publication is not a successfully recovered DLT event.

---

# 39. Low-Cardinality DLT Reason Tag

The DLT metric has a controlled `reason` tag:

```text
reason="processing"
reason="deserialization"
```

Prometheus exposes separate time series:

```promql
climbing_kafka_dlt_events_total{reason="processing"}
climbing_kafka_dlt_events_total{reason="deserialization"}
```

The classifier walks the exception cause chain to identify a `DeserializationException`.

Two runtime experiments verified both branches:

```text
Kafka Retry Test
→ listener-processing failure
→ reason="processing"

malformed payload injected directly into course-events
→ ErrorHandlingDeserializer / DeserializationException
→ reason="deserialization"
```

Low-cardinality categories are preferred over tags such as `eventId`, offset, student name or raw exception messages.

Recent DLT events by reason:

```promql
sum by (reason) (
  increase(
    climbing_kafka_dlt_events_total{
      job="enrollment-service"
    }[5m]
  )
)
```

---

# 40. Duplicate Kafka Event Counter

The idempotency flow already stores processed event IDs in MongoDB. A custom counter records the branch where a duplicate is detected and skipped.

Micrometer:

```text
climbing.kafka.duplicate.events
```

Prometheus:

```text
climbing_kafka_duplicate_events_total
```

The metric increments only when:

```text
repository.existsById(eventId) == true
        ↓
duplicate processing is skipped
        ↓
counter +1
```

The counter is registered once when `ProcessedKafkaEventService` is constructed.

The runtime test published the same valid Kafka event twice with the same `eventId`:

```text
first delivery
→ processed normally
→ eventId stored

second delivery
→ duplicate detected
→ skipped
→ duplicate counter +1
```

Recent duplicates:

```promql
increase(
  climbing_kafka_duplicate_events_total{
    job="enrollment-service"
  }[5m]
)
```

The MongoDB `_id` uniqueness constraint remains the final concurrency safety net because a pre-check such as `existsById(...)` alone cannot eliminate every check-then-insert race.

---

# 41. Business Metrics and Hexagonal Architecture

Business instrumentation must not make the framework-free application service depend directly on Micrometer.

The application therefore defines an outbound port:

```text
EnrollmentMetricsPort
```

Architecture:

```text
CreateEnrollmentService
        ↓
EnrollmentMetricsPort
        ↑
MicrometerEnrollmentMetricsAdapter
        ↓
Micrometer Counter / MeterRegistry
```

The application layer knows only its own port; the technical adapter owns the Micrometer dependency.

The metrics port currently records:

```text
enrollmentCreationAttempted()
enrollmentCreated()
courseValidationFailed()
```

Business flow:

```text
createEnrollment() entered
        ↓
creation attempt +1
        ↓
Course exists?
   │
   ├── NO
   │    ↓
   │ course validation failure +1
   │    ↓
   │ 404
   │
   └── YES
        ↓
      persist Enrollment
        │
        ├── persistence fails
        │    ↓
        │ created counter unchanged
        │
        └── persistence succeeds
             ↓
          created +1
```

This placement keeps the metric semantics precise.

---

# 42. Enrollment Business Counters

## Creation attempts

Micrometer:

```text
climbing.enrollment.creation.attempts
```

Prometheus:

```text
climbing_enrollment_creation_attempts_total
```

It increments when the creation use case is entered.

## Successful Enrollment creation

Final Micrometer name:

```text
climbing.enrollments
```

Prometheus:

```text
climbing_enrollments_total
```

It increments only after successful persistence.

During the exercise, the earlier Micrometer name `climbing.enrollments.created` was observed as `climbing_enrollments_total` in the Prometheus exposition. Using `climbing.enrollments` makes the Micrometer-to-Prometheus mapping easier to understand.

## Course-validation failures

Micrometer:

```text
climbing.enrollment.course.validation.failures
```

Prometheus:

```text
climbing_enrollment_course_validation_failures_total
```

This metric increments only when Course validation says the referenced Course does not exist. It does not represent Course Service unavailability, MongoDB failures, or HTTP validation rejected before the use case.

---

# 43. Grafana Business Metrics

A fourth dashboard row is now used:

```text
Enrollment Service — Business Metrics
```

Panels:

```text
├── Enrollments Created — Last 5m
├── Course Validation Failures — Last 5m
└── Enrollment Creation Success Rate — Last 15m
```

## Enrollments Created — Last 5m

```promql
increase(
  climbing_enrollments_total{
    job="enrollment-service"
  }[5m]
)
```

Recommended display: `Stat`, unit `short`, decimals `0`, min `0`.

## Course Validation Failures — Last 5m

```promql
increase(
  climbing_enrollment_course_validation_failures_total{
    job="enrollment-service"
  }[5m]
)
```

Recommended display: `Stat`, unit `short`, decimals `0`, min `0`.

## Enrollment Creation Success Rate — Last 15m

The denominator is a real creation-attempt counter rather than the sum of only known success/failure categories.

```promql
(
  100 *
  increase(
    climbing_enrollments_total{
      job="enrollment-service"
    }[15m]
  )
  /
  increase(
    climbing_enrollment_creation_attempts_total{
      job="enrollment-service"
    }[15m]
  )
)
and on()
(
  increase(
    climbing_enrollment_creation_attempts_total{
      job="enrollment-service"
    }[15m]
  ) > 0
)
```

Meaning:

```text
successfully persisted Enrollments
---------------------------------- × 100
creation use-case attempts
```

The `and on()` condition preserves the distinction:

```text
no attempts
→ No data

attempts occurred but none succeeded
→ 0%
```

This ratio only covers requests that reach `CreateEnrollmentService`; HTTP/Bean Validation failures rejected before the use case are outside this definition.

---

# 44. Sliding Windows and Low-Traffic Volatility

A moving range such as `increase(metric[5m])` continuously changes its dataset:

```text
time ───────────────────────────────►

      |--------- last 5 min --------|
      ↑                             ↑
 old observations leave        new observations enter
```

Therefore a success percentage can temporarily decrease while new successful requests are being created if older successful observations leave the selected window.

There is also a small timing gap between `attempt +1` and `successful persistence → created +1`; a Prometheus scrape can occur between those two events.

With very low local traffic, the 15-minute ratio window is easier to interpret than a five-minute window.

---

# 45. Testing Metrics Without Breaking the Architecture

Adding multiple methods to `EnrollmentMetricsPort` means it is no longer a functional interface, so the old one-line lambda fake is no longer valid.

Hand-written tests use a small recording fake with counters for:

```text
attempted
created
validationFailed
```

Mockito tests use:

```java
@Mock
private EnrollmentMetricsPort enrollmentMetricsPort;
```

and can verify semantic interactions:

```text
successful creation
→ attempted called
→ created called
→ validationFailed never

missing Course
→ attempted called
→ validationFailed called
→ created never
```

This keeps application tests independent from Micrometer while still verifying the points where metrics are emitted.

---

# 46. Current Grafana Dashboard Structure

```text
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
├── Kafka Listener Failure Rate
├── Kafka DLT Events
└── Kafka Duplicate Events Skipped

Enrollment Service — Business Metrics
├── Enrollments Created
├── Course Validation Failures
└── Enrollment Creation Success Rate
```

---

# 47. Local Observability Architecture

Current learning setup:

```text
Windows host
│
├── Course Service
│      localhost:8080
│      │
│      └── OTLP traces → localhost:4318
│
├── Enrollment Service
│      localhost:8081
│      │
│      ├── /actuator/prometheus
│      └── OTLP traces → localhost:4318
│
└── Docker
       │
       ├── Prometheus
       │      localhost:9090
       │      │
       │      └── scrapes host.docker.internal:8081
       │
       ├── Tempo
       │      localhost:3200   query API
       │      localhost:4317   OTLP gRPC
       │      localhost:4318   OTLP HTTP
       │
       └── Grafana
              localhost:3000
              │
              ├── Prometheus → host.docker.internal:9090
              └── Tempo      → host.docker.internal:3200
```

Prometheus, Tempo and Grafana were intentionally introduced as standalone containers first so the scrape, export and query relationships are visible while learning.

A later project step can integrate them into Docker Compose, where service-to-service names would replace `host.docker.internal` where appropriate.

---

# 48. Current Mental Model

```text
Application behavior
        │
        ├── metrics
        │     ↓
        │  Micrometer / Actuator
        │     ↓
        │  Prometheus
        │     ↓
        │  PromQL
        │     ↓
        │  Grafana dashboards
        │
        ├── traces
        │     ↓
        │  Micrometer Tracing / Observation
        │     ↓
        │  OpenTelemetry
        │     ↓
        │  OTLP
        │     ↓
        │  Tempo
        │     ↓
        │  Grafana Explore
        │
        └── logs
              ↓
           traceId / spanId correlation
```

Current observability coverage:

```text
Traffic        → HTTP request rate
Errors         → HTTP 5xx + Kafka listener failures
Latency        → HTTP p50/p95/p99 + Kafka listener time
Saturation     → CPU + heap + GC
Messaging      → Kafka lag + partitions + throughput + DLT + duplicates
Business       → attempts + creations + validation failures + success rate
Tracing        → HTTP + Kafka distributed traces
Logs           → structured JSON → Alloy → Loki
Correlation    → Loki ↔ Tempo via traceId
```

Completed: custom metrics, distributed tracing, structured logging, centralized logging, SLIs / SLOs and Grafana alerting.

Next:

```text
API versioning
    ↓
Performance
    ↓
Scalability
```

---

# 49. Distributed Tracing Fundamentals

Metrics answer aggregate questions, but they do not show how one individual operation moved through a distributed system.

A trace represents an end-to-end operation:

```text
Trace
└── Span
    ├── child span
    └── child span
```

Each trace has a `traceId` shared by all spans that belong to the same distributed operation. Each individual span has its own `spanId`, and a child span also carries the identity of its parent.

```text
Trace ID = ABC123

Enrollment HTTP SERVER span
├── Security span
└── Course HTTP CLIENT span
    └── Course HTTP SERVER span
```

All spans use the same `traceId`, while every span has a different `spanId`.

---

# 50. Micrometer Tracing, OpenTelemetry and Tempo

The services use Spring Boot's OpenTelemetry starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

Current local tracing configuration:

```properties
management.tracing.sampling.probability=1.0
management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
management.opentelemetry.tracing.export.otlp.transport=http
management.otlp.metrics.export.enabled=false
```

The local architecture intentionally separates metrics and traces:

```text
Metrics
Application
    ↓
Micrometer
    ↓
/actuator/prometheus
    ↓
Prometheus
    ↓
Grafana

Traces
Application
    ↓
Micrometer Tracing / Observation
    ↓
OpenTelemetry
    ↓
OTLP HTTP
    ↓
Tempo
    ↓
Grafana
```

`management.otlp.metrics.export.enabled=false` is important in this setup because Prometheus is the metrics backend while Tempo receives traces only. Without it, the OpenTelemetry starter may also try to export metrics to `http://localhost:4318/v1/metrics`, which is not the metrics path used by this Tempo setup.

---

# 51. Tempo Local Setup

Tempo currently runs as:

```text
grafana/tempo:3.0.3
```

Relevant ports:

```text
3200 → Tempo HTTP query API
4317 → OTLP gRPC ingestion
4318 → OTLP HTTP ingestion
```

The project configuration is:

```text
observability/tempo/tempo.yaml
```

The local Tempo storage backend uses a Docker volume. Because the Tempo image runs as non-root UID `10001`, the learning setup explicitly gives that UID ownership of the volume before starting Tempo.

Readiness check:

```powershell
curl.exe http://localhost:3200/ready
```

Expected:

```text
ready
```

The command-oriented setup and TraceQL examples are also captured in `cheatsheets/AE_Tempo.md`.

---

# 52. Grafana Tempo Data Source

Grafana runs in Docker while Tempo is exposed on the Windows host. Therefore the Tempo data-source URL is:

```text
http://host.docker.internal:3200
```

and not `http://localhost:3200`, because `localhost` inside the Grafana container refers to the Grafana container itself.

```text
Grafana
    ↓
Tempo data source
    ↓
host.docker.internal:3200
    ↓
Tempo
```

Grafana Explore can then search and visualize trace waterfalls.

---

# 53. HTTP Distributed Trace Propagation

The synchronous Enrollment → Course call was used to verify cross-JVM propagation.

```text
enrollment-service
└── POST /enrollments
    └── HTTP CLIENT GET
        └── course-service
            └── GET /jpa/courses/{id}
```

The trace context travels through standard HTTP trace headers. Spring instrumentation injects and extracts that context automatically; application code does not manually parse or create `traceparent`.

The verified trace demonstrated:

```text
Enrollment SERVER span
    ↓
Enrollment CLIENT span
    ↓
same trace context
    ↓
Course SERVER span
```

The Course server span uses the Enrollment client span as its parent across the network boundary.

The Course application is explicitly named:

```properties
spring.application.name=course-service
```

so Tempo / Grafana display the downstream service clearly. Spring Security observations also appear as child spans, including authentication and authorization work.

---

# 54. Kafka Distributed Trace Propagation

Kafka introduces an asynchronous boundary.

```text
POST /jpa/courses
        ↓
course-service
        ↓
KafkaTemplate
        ↓
course-events
        ↓
@KafkaListener
        ↓
enrollment-service
```

Producer observation is enabled in Course Service:

```properties
spring.kafka.template.observation-enabled=true
```

Consumer observation is enabled in Enrollment Service:

```properties
spring.kafka.listener.observation-enabled=true
```

The trace context is propagated through Kafka record headers:

```text
course-service
└── course-events send
        │
        │ trace context in Kafka headers
        ▼
    course-events
        │
        ▼
enrollment-service
└── course-events process
```

Both producer and consumer spans belong to the same distributed trace even though they execute in different JVMs at different times.

This was verified alongside normal Kafka behavior:

```text
producer published record
consumer group offset advanced
consumer lag returned to 0
@KafkaListener logged the consumed event
consumer span appeared in the same Tempo trace
```

---

# 55. Kafka Observation and Existing Metrics

Before observation is enabled, Spring Kafka can expose legacy Micrometer timers.

With Observation enabled:

```text
Kafka Observation
    ↓
metrics
+
traces
+
context propagation
```

The exact metric names / labels available from Spring Kafka can therefore change after enabling Observation. Existing Grafana Kafka panels should be rechecked after this change instead of assuming that the previous listener-timer labels remain identical.

The custom application counters for DLT and duplicate-event handling remain independent business / technical instrumentation.

---

# 56. Understanding the Asynchronous Trace Gap

A Kafka trace can contain a visible time gap between `course-events send` and `course-events process`:

```text
producer span ends
      │
      │ no active instrumented span
      │
consumer span begins
```

That entire gap must not automatically be called "Kafka broker latency". It can contain several things:

```text
broker storage
    ↓
record waiting in partition
    ↓
consumer poll / fetch cycle
    ↓
consumer thread scheduling
    ↓
framework / deserialization work
    ↓
listener observation starts
```

Tracing tells us that no instrumented span covers that interval; it does not by itself attribute every millisecond to one component.

Another important consequence is that the distributed trace can live longer than the original HTTP request:

```text
HTTP request starts
    ↓
Kafka event published
    ↓
HTTP request finishes

... asynchronous time ...

Kafka consumer processes event
    ↓
distributed trace finishes
```

A trace therefore represents the distributed operation, not necessarily the lifetime of one synchronous request.

---

# 57. Trace / Log Correlation

Micrometer Tracing adds the current tracing identifiers to the logging MDC.

The project verified a Course producer log and an Enrollment consumer log with the same trace ID:

```text
Course producer
traceId = 55d9f80db7338a3e41de62143ddfd239
spanId  = abfd304c1d02a229

Enrollment consumer
traceId = 55d9f80db7338a3e41de62143ddfd239
spanId  = b6ba9bf37af32aa1
```

Interpretation:

```text
same traceId
→ same distributed operation

different spanId
→ different spans in that operation
```

This creates the basic correlation workflow:

```text
Log entry
    ↓
traceId
    ↓
Tempo
    ↓
complete distributed trace
```

The current plain-text console format already contains useful trace/span correlation, so no custom correlation pattern is required before moving to structured logging.

---

# 58. Structured JSON Logging

Enrollment Service writes Logstash-style JSON to console and file:

```properties
logging.structured.format.console=logstash
logging.file.name=logs/enrollment-service.log
logging.structured.format.file=logstash
```

SLF4J key/value logging exposes business data as real fields:

```java
log.atInfo()
        .addKeyValue("eventType", event.eventType())
        .addKeyValue("courseId", event.courseId())
        .addKeyValue("partition", record.partition())
        .addKeyValue("offset", record.offset())
        .log("CourseCreatedEvent consumed");
```

Tracing MDC values remain JSON fields:

```json
{"message":"CourseCreatedEvent consumed","courseId":82,"traceId":"...","spanId":"..."}
```

Keep `traceId`, `spanId`, `eventId` and `courseId` out of low-cardinality metric/Loki labels.

---

# 59. Centralized Logging — Alloy and Loki

```text
Enrollment Service
    ↓
logs/enrollment-service.log
    ↓
Grafana Alloy
    ↓
Loki :3100
    ↓
Grafana
```

Project files:

```text
observability/alloy/config.alloy
observability/loki/loki-config.yaml
```

Endpoints:

```text
Loki  → http://localhost:3100
Alloy → http://localhost:12345
```

Basic LogQL:

```logql
{job="enrollment-service"} | json
{job="enrollment-service"} | json | level="INFO"
{job="enrollment-service"} | json | eventType="COURSE_CREATED"
{job="enrollment-service"} | json | traceId="..."
```

Operational commands: `cheatsheets/AE_Loki.md`.

---

# 60. Loki → Tempo

Loki derived field:

```text
Name:        TraceID
Regex:       "traceId":"(\w+)"
Data source: Tempo
Query:       ${__value.raw}
```

```text
Loki log
    ↓ traceId
Tempo trace
```

`traceId` stays a JSON field, not a high-cardinality Loki stream label.

---

# 61. Tempo → Loki

Tempo **Trace to logs**:

```text
Data source: Loki
Tag mapping: service.name → service
Filter by trace ID: ON
```

```text
Tempo span
    ↓ service + traceId
Loki logs
```

Bidirectional correlation:

```text
Loki ── traceId ──► Tempo
  ▲                  │
  └──── logs ────────┘
```

---

# 62. SLIs / SLOs

Core model:

```text
SLI → what is measured
SLO → target for the SLI
SLA → external/business commitment
Error budget → allowed unreliability
```

Current learning availability objective:

```text
SLI: non-5xx requests / total requests
SLO: 99%
Error budget: 1%
Production concept: rolling 30-day window
Local exercise: short PromQL windows
```

Availability SLI:

```promql
100 *
(
  1 -
  (
    sum(rate(http_server_requests_seconds_count{
      job="enrollment-service",
      status=~"5.."
    }[5m]))
    /
    sum(rate(http_server_requests_seconds_count{
      job="enrollment-service"
    }[5m]))
  )
)
```

4xx responses are not treated as service-availability failures for this SLI; 5xx responses are.

---

# 63. Latency SLI

Learning objective:

```text
95% of successful /enrollments requests
must complete in ≤ 500 ms
```

The exact threshold is added as a Micrometer SLO bucket:

```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.slo.http.server.requests=500ms
```

Latency SLI:

```promql
100 *
sum(
  rate(
    http_server_requests_seconds_bucket{
      job="enrollment-service",
      uri="/enrollments",
      status=~"2..",
      le="0.5"
    }[5m]
  )
)
/
sum(
  rate(
    http_server_requests_seconds_count{
      job="enrollment-service",
      uri="/enrollments",
      status=~"2.."
    }[5m]
  )
)
```

Difference:

```text
p95 latency
→ 95% finished within HOW LONG?

Latency SLI
→ HOW MANY % finished within 500 ms?
```

---

# 64. Error Budget and Burn Rate

For the `99%` availability SLO:

```text
Allowed error ratio = 1% = 0.01
```

Burn rate:

```text
actual error ratio
------------------
allowed error ratio
```

PromQL:

```promql
(
  sum(rate(http_server_requests_seconds_count{
    job="enrollment-service",
    status=~"5.."
  }[5m]))
  /
  sum(rate(http_server_requests_seconds_count{
    job="enrollment-service"
  }[5m]))
)
/
0.01
```

Interpretation:

```text
0   → no budget burn
1   → burning at the allowed rate
2   → burning 2x too fast
7.69 → burning 7.69x too fast
```

Grafana panels implemented:

```text
Availability SLI
Latency SLI ≤ 500 ms
Error Budget Consumed
Error Budget Remaining
Burn Rate — 5m
Burn Rate — 1h
```

---

# 65. Multi-Window Burn-Rate Alerts

Fast burn:

```text
5m burn rate > 14.4
AND
1h burn rate > 14.4
```

Slow burn:

```text
30m burn rate > 6
AND
6h burn rate > 6
```

Purpose:

```text
Fast burn → severe current degradation
Slow burn → smaller but sustained degradation
```

Grafana alert expressions use the Prometheus results directly because the burn-rate queries already return single values:

```text
A = short-window burn rate
B = long-window burn rate
C = $A > threshold && $B > threshold
```

`C` is the alert condition. The legacy Classic condition and unnecessary Reduce expressions are not used.

---

# 66. Grafana Alerting and Webhook Delivery

Alert labels:

```text
service=enrollment-service
slo=availability
severity=critical | warning
```

Notification path:

```text
Prometheus
    ↓
Grafana alert rule
    ↓
notification policy
    ↓
webhook contact point
    ↓
observability/other/webhook_receiver.py
```

Local webhook URL from the Grafana container:

```text
http://host.docker.internal:8085/
```

The Python receiver is used instead of Windows `HttpListener`, which required an HTTP URL reservation / elevated shell for the attempted `http://+:8085/` binding.

Real `FIRING` alert delivery to the Python receiver has been verified.

---


[Back to README](../README.md)
