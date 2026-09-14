package com.rubenmarin.enrollmentservice.config;
//
// A Circuit Breaker protects a service from repeatedly calling another service that is currently failing.
// When repeated failures indicate that a dependency is unhealthy,
// the Circuit Breaker temporarily stops calls to that dependency and fails fast instead.
// After a configured period, it allows a test request to check whether the dependency has recovered.
//
// It has three states:
//  - CLOSED: calls are allowed and failures are monitored.
//  - OPEN: calls are blocked because the dependency is considered unhealthy.
//  - HALF-OPEN: a limited test call is allowed to check whether the dependency has recovered.
//
// Its main purpose is to reduce unnecessary network calls, protect resources,
// avoid cascading failures, and allow the system to recover gracefully.
//
// Timeouts limit how long a single call waits.
// Retries repeat a failed call when the failure may be temporary.
// A Circuit Breaker stops making calls when repeated failures show that the dependency is currently unhealthy.

//Enrollment Service
//        │
//        ▼
//Circuit Breaker
//        ├── CLOSED
//        │      ↓
//        │   CourseClient
//        │      ↓
//        │   Course Service
//        │
//        ├── OPEN
//        │      X
//        │   no HTTP call
//        │   fail fast
//        │
//        └── HALF-OPEN
//               ↓
//          test request
//               │
//        ┌──────┴──────┐
//        ▼             ▼
//      success        failure
//        │             │
//        ▼             ▼
//      CLOSED        OPEN


import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<FrameworkRetryCircuitBreakerFactory> courseServiceCircuitBreakerCustomizer() {

        // Circuit Breaker configuration for "courseService"
        //retryPolicy = 0 retries
        //    → the circuit breaker itself will not add extra retries
        //openTimeout = 10s
        //    → once OPEN, keep rejecting calls for 10 seconds
        //    → then allow a HALF-OPEN test call
        //resetTimeout = 30s
        //    → if no failures occur for 30 seconds, its tracked failure state can reset

        return factory -> factory.configure(
                builder -> builder
                        // 0 coz CourseClient is already retryable(2)
                        .retryPolicy(RetryPolicy.withMaxRetries(0))
                        .openTimeout(Duration.ofSeconds(10))
                        .resetTimeout(Duration.ofSeconds(30))
                        .build(),
                "courseService"
        );
    }
}
