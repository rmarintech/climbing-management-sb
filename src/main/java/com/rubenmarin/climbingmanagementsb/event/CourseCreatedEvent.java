package com.rubenmarin.climbingmanagementsb.event;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new Course has been successfully created.
 *
 * This is an event contract, not a persistence entity or REST DTO.
 */

//eventId
//   → uniquely identifies this event
//   → later useful for idempotency / deduplication
//occurredAt
//   → tells consumers when the business event happened

public record CourseCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long courseId,
        String name,
        Double price,
        Difficulty difficulty
) {

    public static CourseCreatedEvent of(
            Long courseId,
            String name,
            Double price,
            Difficulty difficulty
    ) {
        return new CourseCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                courseId,
                name,
                price,
                difficulty
        );
    }
}