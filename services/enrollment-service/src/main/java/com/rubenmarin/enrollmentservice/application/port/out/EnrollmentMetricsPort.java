package com.rubenmarin.enrollmentservice.application.port.out;

public interface EnrollmentMetricsPort {

    void enrollmentCreated();

    void courseValidationFailed();

    void enrollmentCreationAttempted();
}
