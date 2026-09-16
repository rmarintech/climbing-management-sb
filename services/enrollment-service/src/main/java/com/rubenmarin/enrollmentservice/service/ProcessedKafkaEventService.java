package com.rubenmarin.enrollmentservice.service;

import com.rubenmarin.enrollmentservice.document.ProcessedKafkaEventDocument;
import com.rubenmarin.enrollmentservice.event.CourseCreatedEvent;
import com.rubenmarin.enrollmentservice.repository.ProcessedKafkaEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessedKafkaEventService {

    private final ProcessedKafkaEventRepository repository;

    public ProcessedKafkaEventService(ProcessedKafkaEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks whether this event was already successfully processed.
     */
    public boolean wasAlreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    /**
     * Marks an event as successfully processed.
     *
     * insert() is intentional:
     * MongoDB _id uniqueness prevents duplicate event IDs.
     */
    public void markAsProcessed(CourseCreatedEvent event) {

        repository.insert(
                new ProcessedKafkaEventDocument(
                        event.eventId(),
                        event.eventType(),
                        Instant.now()
                )
        );
    }
}