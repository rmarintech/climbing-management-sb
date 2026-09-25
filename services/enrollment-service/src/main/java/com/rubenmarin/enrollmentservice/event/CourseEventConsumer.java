package com.rubenmarin.enrollmentservice.event;

import com.rubenmarin.enrollmentservice.service.ProcessedKafkaEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CourseEventConsumer.class);

    private final ProcessedKafkaEventService processedKafkaEventService;

    public CourseEventConsumer(ProcessedKafkaEventService processedKafkaEventService) {
        this.processedKafkaEventService = processedKafkaEventService;
    }

    @KafkaListener(topics = "course-events")
    public void consume(ConsumerRecord<String, CourseCreatedEvent> record) {

        CourseCreatedEvent event = record.value();

        if (processedKafkaEventService.wasAlreadyProcessed(event.eventId())) {

            log.atWarn()
                    .addKeyValue("eventId", event.eventId())
                    .addKeyValue("eventType", event.eventType())
                    .addKeyValue("courseId", event.courseId())
                    .addKeyValue("partition", record.partition())
                    .addKeyValue("offset", record.offset())
                    .log("Duplicate Kafka event skipped");
            return;
        }

        log.atInfo()
                .addKeyValue("key", record.key())
                .addKeyValue("eventType", event.eventType())
                .addKeyValue("eventVersion", event.eventVersion())
                .addKeyValue("sourceService", event.sourceService())
                .addKeyValue("courseId", event.courseId())
                .addKeyValue("partition", record.partition())
                .addKeyValue("offset", record.offset())
                .addKeyValue("courseName", event.name())
                .addKeyValue("difficulty", event.difficulty())
                .log("CourseCreatedEvent received");

        // Learning experiment:
        //
        // Set the condition to true to simulate a retryable
        // business-processing failure and observe:
        //
        //  attempt 1
        //      ↓
        //  retry 1
        //      ↓
        //  retry 2
        //      ↓
        //     DLT
        //
        // Keep disabled during normal execution.
        if (false && "Kafka Retry Test".equals(event.name())) {
            log.atError()
                    .addKeyValue("courseId", event.courseId())
                    .addKeyValue("partition", record.partition())
                    .addKeyValue("offset", record.offset())
                    .log("Simulated processing failure");

            throw new RuntimeException("Simulated Kafka processing failure");
        }

        // Process the event here.

        processedKafkaEventService.markAsProcessed(event);

        log.atInfo()
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("courseId", event.courseId())
                .addKeyValue("partition", record.partition())
                .addKeyValue("offset", record.offset())
                .log("CourseCreatedEvent processed");
    }
}

/**
 * DESERIALIZATION FAILURE
 */
//      bad bytes / incompatible schema
//                    ↓
//      ErrorHandlingDeserializer
//                    ↓
//      DeserializationException
//                    ↓
//              non-retryable
//                    ↓
//                   DLT

/**
 * PROCESSING FAILURE
 */
//              valid event
//                   ↓
//          @KafkaListener
//                  ↓
//          RuntimeException
//                  ↓
//                 retry
//                  ↓
//                  retry
//                  ↓
//              still fails
//                  ↓
//                  DLT