package com.rubenmarin.enrollmentservice.application.port.in;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

public interface CreateEnrollmentUseCase {

    Enrollment createEnrollment(CreateEnrollmentCommand command);
}