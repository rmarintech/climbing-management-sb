package com.rubenmarin.enrollmentservice.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local representation of the CourseCreated event consumed from Kafka.
 *
 * This contract must stay compatible with the event published
 * by the Course Service.
 */

/**
 * Local representation of the CourseCreated event consumed from Kafka.
 *
 * This contract must stay compatible with the event published
 * by the Course Service.
 *
 * We intentionally use String for difficulty instead of importing
 * the Course Service's Difficulty enum.
 *
 * Bad coupling:
 *
 * Enrollment Service
 *        ↓
 * imports Course Service model.Difficulty
 *        ↓
 * consumer depends on producer's internal Java model
 *
 * Better:
 *
 * Course Service
 *        ↓
 * publishes compatible JSON
 *        ↓
 * Kafka
 *        ↓
 * Enrollment Service
 *        ↓
 * owns its own representation
 */

public record CourseCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long courseId,
        String name,
        Double price,
        String difficulty
) {
}