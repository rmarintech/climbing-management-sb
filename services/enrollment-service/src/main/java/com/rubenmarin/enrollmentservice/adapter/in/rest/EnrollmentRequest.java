package com.rubenmarin.enrollmentservice.adapter.in.rest;

public record EnrollmentRequest(
        Long courseId,
        String studentName
) {
}
