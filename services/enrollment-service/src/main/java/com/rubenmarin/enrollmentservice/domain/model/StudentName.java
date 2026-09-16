package com.rubenmarin.enrollmentservice.domain.model;

public record StudentName(String value) {

    public StudentName {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Student name cannot be null or blank");
        }
        value = value.trim();
    }
}