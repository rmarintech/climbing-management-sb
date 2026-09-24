package com.rubenmarin.enrollmentservice.service;

import com.rubenmarin.enrollmentservice.document.ProcessedKafkaEventDocument;
import com.rubenmarin.enrollmentservice.event.CourseCreatedEvent;
import com.rubenmarin.enrollmentservice.repository.ProcessedKafkaEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessedKafkaEventService {

    private final ProcessedKafkaEventRepository repository;
    private final Counter duplicatedCounter;


    public ProcessedKafkaEventService(
            ProcessedKafkaEventRepository repository,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.duplicatedCounter = Counter.builder("climbing.kafka.duplicate.events")
                .description("Number of duplicate Kafka events skipped")
                .register(meterRegistry);
    }

    /**
     * Checks whether this event was already successfully processed.
     */
    public boolean wasAlreadyProcessed(UUID eventId) {
        boolean processed = repository.existsById(eventId);
        if (processed) {
            duplicatedCounter.increment();
        }
        return processed;
    }

    /**
     * Marks an event as successfully processed.
     * <p>
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