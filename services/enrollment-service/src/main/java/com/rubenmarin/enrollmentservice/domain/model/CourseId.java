package com.rubenmarin.enrollmentservice.domain.model;

public record CourseId(Long value) {

    public CourseId {

        if (value == null) {
            throw new IllegalArgumentException("Course id cannot be null");
        }

        if (value <= 0) {
            throw new IllegalArgumentException("Course id must be positive");
        }
    }
}
