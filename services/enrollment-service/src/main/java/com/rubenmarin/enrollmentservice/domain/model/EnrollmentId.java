package com.rubenmarin.enrollmentservice.domain.model;

public record EnrollmentId(String value) {

    public EnrollmentId {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enrollment id cannot be null or blank");
        }
    }
}