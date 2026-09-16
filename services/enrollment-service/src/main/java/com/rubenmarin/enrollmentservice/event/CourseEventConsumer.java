package com.rubenmarin.enrollmentservice.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CourseEventConsumer.class);

    @KafkaListener(topics = "course-events")
    public void consume(ConsumerRecord<String, CourseCreatedEvent> record) {

        CourseCreatedEvent event = record.value();

        log.info(
                "CourseCreatedEvent consumed. key={}, courseId={}, partition={}, offset={}, name={}, difficulty={}",
                record.key(),
                event.courseId(),
                record.partition(),
                record.offset(),
                event.name(),
                event.difficulty()
        );
    }
}