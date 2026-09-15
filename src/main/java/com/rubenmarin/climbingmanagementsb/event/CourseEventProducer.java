package com.rubenmarin.climbingmanagementsb.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CourseEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CourseEventProducer.class);
    private static final String COURSE_EVENTS_TOPIC = "course-events";
    private final KafkaTemplate<String, CourseCreatedEvent> kafkaTemplate;

    public CourseEventProducer(KafkaTemplate<String, CourseCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCourseCreated(CourseCreatedEvent event) {

        // courseId is used as the Kafka record key.
        //
        // Later, when the topic has multiple partitions, events with the
        // same courseId will normally be routed to the same partition,
        // preserving ordering for that Course.
        String key = event.courseId().toString();

        kafkaTemplate
                .send(COURSE_EVENTS_TOPIC, key, event)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Failed to publish CourseCreatedEvent. courseId={}",
                                event.courseId(),
                                exception
                        );
                        return;
                    }

                    log.info(
                            "CourseCreatedEvent published. courseId={}, partition={}, offset={}",
                            event.courseId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}