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

Most metrics used so far are automatically provided by frameworks:
```text
HTTP metrics
JVM / process metrics
MongoDB metrics
Kafka client metrics
Spring Kafka listener metrics
```

They do not answer every application-specific question. Examples:
```text
How many records were sent to the DLT?
How many enrollments were created?
How many Course validations failed?
How many duplicate Kafka events were skipped?
```

This introduces the next milestone:
```text
Custom Micrometer metrics
```

A planned first custom counter is a dedicated DLT-event metric, for example:
```text
climbing.kafka.dlt.events
```

The exact custom metric is not implemented yet.

---

# 37. Local Observability Architecture

Current learning setup:

```text
Windows host
│
├── Course Service
│      localhost:8080
│
├── Enrollment Service
│      localhost:8081
│      │
│      └── /actuator/prometheus
│
└── Docker
       │
       ├── Prometheus
       │      localhost:9090
       │      │
       │      └── scrapes host.docker.internal:8081
       │
       └── Grafana
              localhost:3000
              │
              └── queries Prometheus through
                  host.docker.internal:9090
```

Prometheus and Grafana were intentionally introduced as standalone containers first so the scrape/query relationships are visible while learning.

A later project step can integrate them into Docker Compose, where service-to-service names would replace `host.docker.internal`.

---

# 38. Current Mental Model

```text
Application behavior
        ↓
Micrometer instrumentation
        ↓
Actuator / Prometheus exposition
        ↓
Prometheus scrape
        ↓
time-series storage
        ↓
PromQL
        ↓
Grafana panels
        ↓
operational understanding
```

Current observability coverage:

```text
Traffic      → HTTP request rate
Errors       → HTTP 5xx + Kafka listener failures
Latency      → HTTP average + p50/p95/p99 + Kafka listener time
Saturation   → CPU + heap + GC
Messaging    → lag + partitions + throughput + listener behavior
```

The next active step is custom Micrometer / business metrics, beginning with a dedicated DLT-event counter. The broader stages after that are distributed tracing, structured / centralized logging, trace-log correlation, SLIs/SLOs and alerting.

---

[Back to README](../README.md)
