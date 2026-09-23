PROMETHEUS COMMANDS — CLIMBING MANAGEMENT
============================================================
1. PROMETHEUS
   =============
START PROMETHEUS
----------------

# Run Prometheus in Docker.
#
# Port:
#   localhost:9090 -> Prometheus UI
#
# Configuration:
#   observability/prometheus/prometheus.yml
#
# host.docker.internal allows Prometheus running inside Docker
# to scrape the Enrollment Service running on the Windows host.

docker run `
  --name climbing-prometheus `
-p 9090:9090 `
  -v "${PWD}\observability\prometheus\prometheus.yml:/etc/prometheus/prometheus.yml:ro" `
prom/prometheus:v3.14.0

Prometheus UI:
http://localhost:9090

Enrollment Service Prometheus endpoint:
http://localhost:8081/actuator/prometheus

Prometheus configuration:
observability/prometheus/prometheus.yml

Example:

global:
scrape_interval: 5s

scrape_configs:
- job_name: "enrollment-service"
  metrics_path: "/actuator/prometheus"
  static_configs:
    - targets:
        - "host.docker.internal:8081"
2. SPRING BOOT / MICROMETER CONFIGURATION
   =========================================
Prometheus registry dependency:

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

Expose Actuator endpoints:
management.endpoints.web.exposure.include=health,info,metrics,prometheus

Enable histogram buckets for HTTP requests:
management.metrics.distribution.percentiles-histogram.http.server.requests=true

Without the histogram:
http_server_requests_seconds_count
http_server_requests_seconds_sum
http_server_requests_seconds_max

With histogram enabled:
http_server_requests_seconds_bucket
http_server_requests_seconds_count
http_server_requests_seconds_sum
http_server_requests_seconds_max
3. PROMETHEUS HEALTH / TARGET
   =============================
# Verify that Prometheus is successfully scraping the Enrollment Service.
up{job="enrollment-service"}

Expected:
1

Meaning:

1 -> target is UP and being scraped successfully
0 -> target exists but Prometheus cannot scrape it
4. PROMETHEUS METRIC MODEL
   ==========================
A Prometheus time series is identified by:

metric name
+
label combination
=
unique time series

Example:

http_server_requests_seconds_count{
method="GET",
status="200",
uri="/enrollments"
}

Different labels create different time series:
GET /enrollments 200 -> series A
POST /enrollments 503 -> series B

Spring / Micrometer labels include:

method
status
uri
outcome
error
exception

Prometheus adds target labels such as:

job
instance
5. HTTP REQUEST COUNTER
   =======================
# Show all HTTP request counters.
http_server_requests_seconds_count

# Only /enrollments.

http_server_requests_seconds_count{
uri="/enrollments"
}

The counter represents:
total completed requests since the application started

Example:
http_server_requests_seconds_count = 20

means:
20 requests have been recorded.
6. RATE — REQUESTS PER SECOND
   =============================
# Approximate request rate during the last minute.

rate(
http_server_requests_seconds_count{
uri="/enrollments"
}[1m]
)

rate(...[1m])

means:

approximate counter increase per second
during the last minute

Example:
0.05

means approximately:
0.05 requests / second

Approximately:
0.05 * 60
= 3 requests / minute
7. AGGREGATE REQUEST RATE
   =========================

# Group traffic by HTTP method and status.

sum by (method, status) (
rate(
http_server_requests_seconds_count{
uri="/enrollments"
}[1m]
)
)

Example:

{method="GET", status="200"}   0.05
{method="POST", status="503"}  0.01

sum by(...)

combines multiple time series while preserving
the selected labels.
8. INCREASE — REQUESTS DURING A PERIOD
   ======================================

# Approximate number of requests during the last minute.

sum by (method, status) (
increase(
http_server_requests_seconds_count{
uri="/enrollments"
}[1m]
)
)

increase(...[1m])

means:

approximate counter increase during the last minute

Important:

increase() may return fractional values.

Example:
3.27

does NOT mean that 3.27 requests physically happened.

Prometheus estimates the counter increase using
the available scrape samples and extrapolates
to the boundaries of the selected time range.
9. RATE VS INCREASE
   ===================

rate(counter[1m])

-> approximate increase PER SECOND

increase(counter[1m])

-> approximate TOTAL increase during the minute

Example:

rate = 0.05 requests/sec

approximately:

increase ≈ 3 requests/min
10. INSTANT VECTOR VS RANGE VECTOR
    ==================================

Instant vector:

http_server_requests_seconds_count{
uri="/enrollments"
}

Returns:

latest sample for every matching time series.

Range vector:

http_server_requests_seconds_count{
uri="/enrollments"
}[1m]

Returns:

all samples from the last minute
for every matching time series.

Functions such as:

rate()
increase()

operate on range vectors.
11. AVERAGE HTTP LATENCY
    ========================

HTTP Timer metrics provide:

_count
-> number of requests

_sum
-> accumulated request duration

Therefore:

average latency
=
total duration / number of requests

PromQL:

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

Result unit: SECONDS
Example: 0.0107
means approximately: 10.7 ms
12. FILTER AVERAGE LATENCY
    ==========================
# Successful GET /enrollments latency.
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

# Failed POST /enrollments latency.
rate(
http_server_requests_seconds_sum{
uri="/enrollments",
method="POST",
status="503"
}[1m]
)
/
rate(
http_server_requests_seconds_count{
uri="/enrollments",
method="POST",
status="503"
}[1m]
)
13. SERVER ERROR RATE
    =====================
Error rate: errors / total requests

# Percentage of HTTP 5xx responses.

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

status=~"5.."

is a regular-expression matcher and includes:

500
501
502
503
504
...

Example:0.5 without "* 100"
means:50% error rate

Example:50with "* 100"
means:50% error rate
14. HISTOGRAMS
    ==============

Histograms allow us to understand latency DISTRIBUTION
instead of only average latency.

Histogram metric:
http_server_requests_seconds_bucket

Each bucket contains an upper boundary:
le

means:
less than or equal

Example: le="0.010"

means: request duration <= 10 ms

IMPORTANT:

Histogram buckets are CUMULATIVE.

Example:
le="0.005"   1
le="0.010"   3
le="0.025"   7
le="+Inf"    8

means:
1 request <= 5 ms
3 requests <= 10 ms
7 requests <= 25 ms
8 requests total
15. INSPECT HISTOGRAM BUCKETS
    =============================
http_server_requests_seconds_bucket{
uri="/enrollments",
method="GET",
status="200"
}
16. LATENCY PERCENTILES
    =======================

p50:50% of requests completed within this duration.
p90:90% of requests completed within this duration.
p95:95% of requests completed within this duration.
p99:99% of requests completed within this duration.

Typical ordering:
p50 <= p90 <= p95 <= p99
17. P50 LATENCY
    ===============
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
18. HISTOGRAM_QUANTILE QUERY BREAKDOWN
    ======================================

Example:

histogram_quantile(
0.95,
sum by (le) (
rate(
http_server_requests_seconds_bucket{
uri="/enrollments"
}[5m]
)
)
)

Read it from inside to outside:
1. http_server_requests_seconds_bucket : Select histogram buckets.
2. rate(...[5m]): Calculate bucket growth during the last 5 minutes.
3. sum by (le) : Aggregate time series while preserving the histogram bucket boundary.
4. histogram_quantile(0.95, ...) Estimate the p95 latency.
19. AVERAGE VS PERCENTILES
    ==========================
Average: _sum / _count

Useful for:
general latency level But average can hide slow requests.

Example:
Most requests: 20 ms
One request: 1.2 seconds

Average may look significantly worse without explaining the distribution.

Percentiles reveal the distribution:

p50-> typical / median request
p95-> slow-user experience
p99 -> extreme tail latency
20. CURRENT OBSERVABILITY SIGNALS
    =================================
TRAFFIC
-------
rate(
http_server_requests_seconds_count[...]
)

ERRORS
------
5xx request rate / total request rate

LATENCY
-------
average

p50

p95

p99

These form part of the classic service Golden Signals:

Latency
Traffic
Errors
Saturation
21. CURRENT ENROLLMENT EXAMPLE
    ==============================

Recent GET /enrollments observations:
p50 ≈ 22.4 ms
p90 ≈ 80.5 ms
p95 ≈ 85.0 ms
p99 ≈ 88.6 ms

Interpretation:
50% requests <= ~22 ms
90% requests <= ~81 ms
95% requests <= ~85 ms
99% requests <= ~89 ms

This demonstrates tail latency:
typical request latency != slow-user request latency
