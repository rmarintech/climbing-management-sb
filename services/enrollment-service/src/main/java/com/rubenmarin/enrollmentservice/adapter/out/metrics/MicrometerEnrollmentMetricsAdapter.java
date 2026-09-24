package com.rubenmarin.enrollmentservice.adapter.out.metrics;

import com.rubenmarin.enrollmentservice.application.port.out.EnrollmentMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class MicrometerEnrollmentMetricsAdapter implements EnrollmentMetricsPort {

    private static final Logger log =
            LoggerFactory.getLogger(MicrometerEnrollmentMetricsAdapter.class);

    private final Counter enrollmentCreatedCounter;

    private final Counter courseValidationFailedCounter;

    private final Counter enrollmentCreationAttemptCounter;

    public MicrometerEnrollmentMetricsAdapter(MeterRegistry meterRegistry) {
        // Prometheus climbing_enrollments_total
        this.enrollmentCreatedCounter =
                Counter.builder("climbing.enrollments")
                        .description("Number of successfully created enrollments")
                        .register(meterRegistry);

        this.courseValidationFailedCounter =
                Counter.builder("climbing.enrollment.course.validation.failures")
                        .description("Number of enrollment creation attempts rejected because the Course does not exist")
                        .register(meterRegistry);

        this.enrollmentCreationAttemptCounter =
                Counter.builder("climbing.enrollment.creation.attempts")
                        .description("Number of enrollment creation use-case attempts")
                        .register(meterRegistry);
    }

    @Override
    public void enrollmentCreated() {
        enrollmentCreatedCounter.increment();
    }

    @Override
    public void courseValidationFailed() {
        courseValidationFailedCounter.increment();
    }

    @Override
    public void enrollmentCreationAttempted() {
        enrollmentCreationAttemptCounter.increment();
    }
}
