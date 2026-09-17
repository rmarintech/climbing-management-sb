package com.rubenmarin.enrollmentservice.adapter.in.rest;

public record EnrollmentResponse(
        String id,
        Long courseId,
        String studentName,
        String status
) {
}
