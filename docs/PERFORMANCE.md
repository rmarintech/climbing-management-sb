# Performance

[Back to README](../README.md) · [Project Roadmap](ROADMAP.md)

This document contains the theory, commands, experiments and conclusions from the **Performance** phase of the Enrollment Service. Progress checkboxes remain only in `ROADMAP.md`.

---

# 1. Performance mental model

Performance work in this project follows one rule:

```text
Measure
   ↓
Find the bottleneck or saturation signal
   ↓
Change ONE variable
   ↓
Measure again
```

The goal is not to make random configuration changes. A useful experiment changes one condition at a time and compares the result with a known baseline.

The main concepts are:

```text
Latency
→ How long one request takes.

Throughput
→ How many requests complete per unit of time.

Concurrency
→ How many requests are in progress at the same time.

Resource utilization
→ How much CPU, memory, I/O, connection-pool capacity, etc. is being used.

Saturation
→ The point where additional load mostly creates waiting instead of useful throughput.
```

A typical saturation curve looks like:

```text
load ↑
  ↓
throughput ↑
  ↓
throughput begins to flatten
  ↓
queues / waiting increase
  ↓
latency ↑↑
  ↓
errors or dropped work may eventually appear
```

---

# 2. Why percentiles matter

Average latency is useful but can hide slow requests.

The tests therefore report:

```text
median
p90
p95
p99
max
```

For example:

```text
p95 = 100 ms
```

means approximately 95% of measured requests completed in 100 ms or less.

Tail latency is particularly important because a service can have a reasonable average while a significant minority of requests are much slower.

The existing learning latency objective was reused during this phase:

```text
95% of successful Enrollment API requests ≤ 500 ms
```

That gives a practical criterion for deciding whether a load level is acceptable instead of asking only whether the service still returns `200`.

---

# 3. k6 baseline

Performance scripts live under the Enrollment Service:

```text
services/enrollment-service/performance/
```

The first baseline used one looping virtual user for 30 seconds:

```javascript
export const options = {
    vus: 1,
    duration: '30s',
    summaryTrendStats: [
        'avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};
```

The authenticated request uses the JWT from an environment variable so the token is never committed:

```javascript
export default function () {
    const response = http.get(
        'http://localhost:8081/api/v2/enrollments',
        {
            headers: {
                Authorization: `Bearer ${__ENV.TOKEN}`,
                Accept: 'application/json',
            },
        }
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}
```

A clean one-VU baseline produced approximately:

| Metric | Result |
| --- | ---: |
| Throughput | 116.6 req/s |
| Average | 8.31 ms |
| p95 | 15.26 ms |
| p99 | 29.4 ms |
| Errors | 0% |

A later recovered one-VU sanity run was similarly healthy:

| Metric | Result |
| --- | ---: |
| Throughput | 124.1 req/s |
| Average | 7.8 ms |
| p95 | 11.76 ms |
| p99 | 16.13 ms |
| Errors | 0% |

This also demonstrates why benchmark repetition matters: JVM warm-up, caches, background work and the state of the local machine can change individual results.

---

# 4. Constant-VU load tests

The first scaling experiments increased concurrent looping users.

Representative results were:

| VUs | Throughput | p95 | p99 | Errors |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 116.6 req/s | 15.26 ms | 29.4 ms | 0% |
| 5 | 423.6 req/s | 19.97 ms | 30.63 ms | 0% |
| 10 | 485.5 req/s | 37.67 ms | 69.71 ms | 0% |
| 20 | 500.3 req/s | 69.05 ms | 97.58 ms | 0% |

The curve already showed an important pattern:

```text
1 → 5 VUs
large throughput gain

5 → 10 VUs
smaller throughput gain + more latency

10 → 20 VUs
almost no throughput gain + much more latency
```

This is the **knee of the curve** concept: adding concurrency eventually stops producing proportional throughput.

A later valid 50-VU run produced:

| Metric | Result |
| --- | ---: |
| Throughput | 478.0 req/s |
| Average | 100.86 ms |
| p95 | 202.24 ms |
| p99 | 444.39 ms |
| Max | 1.09 s |
| Errors | 0% |

More concurrency was mostly increasing waiting time rather than useful throughput.

## Fast failures are misleading

One 50-VU run reported roughly `865 req/s`, but 36.79% of requests failed. The headline throughput was therefore not useful as an application-capacity number.

Lesson:

```text
fast failed requests
        ≠
high successful capacity
```

Always interpret throughput together with status codes / error rate.

---

# 5. Stress test

A staged stress scenario gradually increased concurrency:

```javascript
export const options = {
    stages: [
        { duration: '20s', target: 5 },
        { duration: '20s', target: 10 },
        { duration: '20s', target: 20 },
        { duration: '20s', target: 30 },
        { duration: '20s', target: 40 },
        { duration: '20s', target: 50 },
        { duration: '10s', target: 0 },
    ],
};
```

The completed stress run processed 59,733 requests with no HTTP failures:

| Metric | Result |
| --- | ---: |
| Average | 46.01 ms |
| p95 | 107.45 ms |
| p99 | 164.94 ms |
| Max | 752.5 ms |
| Errors | 0% |

The aggregate `459 req/s` value from this run is **not** the throughput at 50 VUs because it averages the whole ramp, including the low-load periods.

For ramp tests, Grafana is useful because throughput and latency can be viewed as a time series while the number of VUs changes.

---

# 6. Closed vs open workload models

## Closed model

Constant VUs use a closed loop:

```text
VU sends request
      ↓
waits for response
      ↓
sends next request
```

If the server slows down, each VU waits longer, so the request rate automatically falls.

That means a closed workload can partially self-throttle.

## Open model

The `constant-arrival-rate` executor models an external arrival rate:

```text
"start N iterations every second"
```

It tries to create the requested rate independently of response latency.

Example:

```javascript
export const options = {
    scenarios: {
        enrollment_api: {
            executor: 'constant-arrival-rate',
            rate: 300,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 100,
            maxVUs: 200,
        },
    },
};
```

This is more useful for asking:

```text
Can the service keep up with 300 req/s?
Can it keep up with 500 req/s?
At what incoming rate does latency become unacceptable?
```

---

# 7. `dropped_iterations`

With an arrival-rate executor, k6 may report:

```text
dropped_iterations
```

These iterations were scheduled but never started. They are different from HTTP failures:

```text
scheduled iteration
      ↓
no VU available / generator cannot start it
      ↓
dropped before HTTP request
```

Therefore:

```text
http_req_failed = 0%
```

does not prove that the full target arrival rate was generated.

This distinction became important in the higher-rate experiments.

---

# 8. Arrival-rate experiments

Representative runs were:

| Target | Actual HTTP rate | p95 | p99 | HTTP errors | Dropped iterations |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 300 req/s | 299.7 req/s | 38.28 ms | 97.14 ms | 0% | 8 |
| 400 req/s | 390.6 req/s | 113.06 ms | 220.85 ms | 0% | 278 |
| 500 req/s | 484.5 req/s | 189.84 ms | 302.85 ms | 0% | 358 |
| 600 req/s | 465.0 req/s | 1.28 s | 1.94 s | 0% | 4,045 |

The 600 req/s run also reached the configured `300` VU ceiling.

The important signal was not an HTTP error spike. It was:

```text
offered load ↑
throughput stops increasing
latency explodes
required concurrency ↑
dropped iterations ↑
```

At 600 req/s offered load:

```text
avg  ≈ 519 ms
p95  ≈ 1.28 s
p99  ≈ 1.94 s
max  ≈ 4.46 s
```

The service still returned `200` for every request that reached it, but the latency objective failed badly.

This demonstrates why overload cannot be defined only as "the service returns 500 errors".

---

# 9. Little's Law

A useful approximation is:

```text
concurrency ≈ throughput × latency
```

For the overloaded 600 req/s experiment:

```text
~465 req/s × ~0.519 s
≈ 241 concurrent requests on average
```

That helps explain why k6 needed hundreds of active VUs once latency grew.

As latency increases, maintaining the same arrival rate requires more simultaneous work.

---

# 10. Tracing overhead experiment

During high load, the Enrollment Service logged an OpenTelemetry warning indicating that the `BatchSpanProcessor` queue was full:

```text
maxQueueSize=2048
```

Meaning:

```text
spans generated faster than exporter drains them
        ↓
queue fills
        ↓
new spans are dropped
```

This proved that the tracing export pipeline itself was saturated, but did **not** by itself prove that tracing was the main HTTP bottleneck.

A controlled experiment changed only tracing sampling.

Representative 20-VU results:

| Metric | Tracing 100% | Tracing disabled, warmed |
| --- | ---: | ---: |
| Throughput | ~500.3 req/s | ~565.7 req/s |
| Average | 38.98 ms | 34.65 ms |
| p95 | 69.05 ms | 58.46 ms |
| p99 | 97.58 ms | 83.73 ms |
| Errors | 0% | 0% |

The first tracing-disabled run was slower because the JVM had just restarted, which reinforced another lesson:

```text
restart
  ↓
JIT / caches / connections cold
  ↓
first benchmark may not be comparable
```

After warm-up, disabling tracing improved throughput and tail latency enough to show that tracing had a measurable cost under load.

For later local tests, a more realistic compromise was selected:

```properties
management.tracing.sampling.probability=0.1
```

The objective is not "disable observability". It is to balance visibility and overhead.

---

# 11. Reproducibility and recovery

After repeated stress tests, a later 300 req/s run was much slower than the original 300 req/s reference:

| Metric | Original 300 req/s | Later 300 req/s |
| --- | ---: | ---: |
| Actual | 299.7 req/s | 297.3 req/s |
| Average | 14.04 ms | 39.56 ms |
| p95 | 38.28 ms | 201.39 ms |
| p99 | 97.14 ms | 517.5 ms |
| Errors | 0% | 0% |

A one-VU sanity test had already returned to normal, so the environment was healthy at low concurrency but still behaved differently under the higher arrival rate.

This is why serious benchmarking needs consistent starting conditions:

```text
same application configuration
same JWT validity
same tracing sampling
same database state
same load generator
same warm-up
same cooldown
same competing local processes
```

On this project, k6, the JVM, Docker Desktop, MongoDB and observability components share one development machine. Therefore the measured numbers describe the **whole local test system**, not an isolated production service.

---

# 12. Load generator vs application capacity

A benchmark can be limited by the client that generates load.

Example warning:

```text
Insufficient VUs, reached 300 active VUs and cannot initialize more
```

This means k6 could not start all scheduled work with the configured worker limit.

Therefore it is necessary to distinguish:

```text
application capacity
        vs
load-generator capacity
        vs
shared-machine contention
```

For this reason, the project does **not** claim an exact production capacity such as "the service can handle X req/s".

The defensible conclusion is narrower:

> In this local environment, the Enrollment Service showed a clear saturation region around the higher 500–600 req/s offered-load experiments, where throughput stopped scaling cleanly and p95 latency exceeded the 500 ms learning objective.

---

# 13. Java Flight Recorder

The next diagnostic step was profiling rather than speculative tuning.

The Enrollment JVM was located with:

```powershell
jcmd -l
```

Example from the project:

```text
10736 com.rubenmarin.enrollmentservice.EnrollmentServiceApplication
```

A 45-second JFR profile was then started:

```powershell
jcmd 10736 JFR.start name=performance settings=profile duration=45s filename=performance.jfr
```

While JFR was recording, the 300 req/s arrival-rate workload was executed.

That run reproduced the degraded behavior:

| Metric | Result |
| --- | ---: |
| Actual | 297.7 req/s |
| Average | 37.78 ms |
| p95 | 215.11 ms |
| p99 | 409.89 ms |
| Errors | 0% |
| Dropped iterations | 61 |

That is useful because the recording captured the JVM while the interesting behavior was actually happening.

JFR can then be used to investigate:

```text
CPU hotspots
thread states
monitor contention / locks
GC activity
allocation pressure
socket / I/O waits
```

The important workflow is:

```text
reproduce problem
      ↓
profile the JVM
      ↓
form a hypothesis
      ↓
change one thing
      ↓
benchmark again
```

The course stops here for deep profiling for now; the JFR workflow is established and can be resumed later if a specific bottleneck needs to be diagnosed.

---

# 14. Key lessons

```text
1. Measure before optimizing.
2. p95 / p99 matter more than average alone.
3. More concurrency does not guarantee more throughput.
4. Failed requests can make throughput look artificially good.
5. Closed workloads can self-throttle.
6. Open arrival-rate workloads expose queueing and saturation more clearly.
7. dropped_iterations are not HTTP failures.
8. Observability has a measurable runtime cost.
9. JVM warm-up and test order can distort comparisons.
10. The load generator is part of the benchmark system.
11. A service can be overloaded while still returning 200 responses.
12. Capacity should be defined against an SLO, not only a crash point.
13. Local benchmark results are not production-capacity guarantees.
14. Profile before tuning thread pools, MongoDB, heap or other internals.
```

---

# 15. Next phase

The next phase is **Scalability**.

The question changes from:

```text
How fast is one service instance?
```

to:

```text
How does the architecture handle more load by adding capacity?
```

That leads into stateless services, multiple replicas, Kubernetes load balancing, HPA behavior, resource requests / limits, database bottlenecks, Kafka consumer scaling, backpressure and caching.

---

[Back to README](../README.md) · [Project Roadmap](ROADMAP.md)
