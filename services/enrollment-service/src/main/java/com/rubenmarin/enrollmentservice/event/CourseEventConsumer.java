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
            log.warn(
                    "Duplicate Kafka event skipped. eventId={}, eventType={}, courseId={}, partition={}, offset={}",
                    event.eventId(),
                    event.eventType(),
                    event.courseId(),
                    record.partition(),
                    record.offset()
            );
            return;
        }


        log.info(
                "CourseCreatedEvent consumed. key={}, eventType={}, eventVersion={}, sourceService={}, courseId={}, partition={}, offset={}, name={}, difficulty={}",
                record.key(),
                event.eventType(),
                event.eventVersion(),
                event.sourceService(),
                event.courseId(),
                record.partition(),
                record.offset(),
                event.name(),
                event.difficulty()
        );

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
            log.error(
                    "Simulated processing failure. courseId={}, partition={}, offset={}",
                    event.courseId(),
                    record.partition(),
                    record.offset()
            );

            throw new RuntimeException("Simulated Kafka processing failure");
        }

        // Process the event here.

        processedKafkaEventService.markAsProcessed(event);
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