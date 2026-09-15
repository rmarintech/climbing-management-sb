package com.rubenmarin.enrollmentservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CourseEventConsumer.class);

    @KafkaListener(topics = "course-events")
    public void consume(CourseCreatedEvent event) {

        log.info(
                "CourseCreatedEvent consumed. courseId={}, name={}, difficulty={}",
                event.courseId(),
                event.name(),
                event.difficulty()
        );
    }
}