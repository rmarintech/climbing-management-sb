package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.FindEnrollmentsUseCase;
import com.rubenmarin.enrollmentservice.application.port.out.FindEnrollmentsPort;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

import java.util.List;

public class FindEnrollmentsService implements FindEnrollmentsUseCase {

    private final FindEnrollmentsPort findEnrollmentsPort;

    public FindEnrollmentsService(FindEnrollmentsPort findEnrollmentsPort) {
        this.findEnrollmentsPort = findEnrollmentsPort;
    }


    @Override
    public List<Enrollment> findAll() {

        return findEnrollmentsPort.findAll();
    }
}

