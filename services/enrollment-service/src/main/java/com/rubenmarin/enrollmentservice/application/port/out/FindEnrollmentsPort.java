package com.rubenmarin.enrollmentservice.application.port.out;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

import java.util.List;

public interface FindEnrollmentsPort {
    List<Enrollment> findAll();
}
