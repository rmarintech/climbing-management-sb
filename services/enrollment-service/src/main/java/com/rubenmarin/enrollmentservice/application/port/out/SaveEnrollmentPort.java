package com.rubenmarin.enrollmentservice.application.port.out;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

public interface SaveEnrollmentPort {

    Enrollment save(Enrollment enrollment);
}