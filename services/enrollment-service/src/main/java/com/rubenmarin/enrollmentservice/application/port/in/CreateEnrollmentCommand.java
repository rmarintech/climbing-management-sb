package com.rubenmarin.enrollmentservice.application.port.in;

public record CreateEnrollmentCommand(
        Long courseId,
        String studentName
) {
}