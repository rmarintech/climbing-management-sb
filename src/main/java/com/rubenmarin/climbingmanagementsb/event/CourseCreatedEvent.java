package com.rubenmarin.climbingmanagementsb.event;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new Course has been successfully created.
 * <p>
 * This is an event contract, not a persistence entity or REST DTO.
 */

//eventId
//   → uniquely identifies this event
//   → later useful for idempotency / deduplication
//occurredAt
//   → tells consumers when the business event happened

public record CourseCreatedEvent(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        String sourceService,
        Instant occurredAt,
        Long courseId,
        String name,
        Double price,
        Difficulty difficulty
) {

    private static final String EVENT_TYPE = "COURSE_CREATED";
    private static final int EVENT_VERSION = 2;
    private static final String SOURCE_SERVICE = "course-service";

    public static CourseCreatedEvent of(
            Long courseId,
            String name,
            Double price,
            Difficulty difficulty
    ) {
        return new CourseCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_VERSION,
                SOURCE_SERVICE,
                Instant.now(),
                courseId,
                name,
                price,
                difficulty
        );
    }
}