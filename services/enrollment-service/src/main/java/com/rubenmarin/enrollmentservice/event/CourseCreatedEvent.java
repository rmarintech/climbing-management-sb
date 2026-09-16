package com.rubenmarin.enrollmentservice.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local representation of the CourseCreated event consumed from Kafka.
 *
 * This contract must stay compatible with the event published
 * by the Course Service.
 *
 */

//Important: the Java types do not have to be identical internally. They only need to be compatible through JSON.
//      Course Difficulty.MEDIUM
//                ↓
//              JSON
//             "MEDIUM"
//                ↓
//          Enrollment String
//              "MEDIUM"
public record CourseCreatedEvent(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        String sourceService,
        Instant occurredAt,
        Long courseId,
        String name,
        Double price,
        String difficulty
) {
}