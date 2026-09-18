package com.rubenmarin.enrollmentservice.application.port.in;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

import java.util.List;

public interface FindEnrollmentsUseCase {

    List<Enrollment> findAll();
}
