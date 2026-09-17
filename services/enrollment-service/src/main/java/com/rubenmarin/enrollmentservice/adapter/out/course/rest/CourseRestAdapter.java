package com.rubenmarin.enrollmentservice.adapter.out.course.rest;

import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.client.CourseClient;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

@Component
public class CourseRestAdapter implements CourseExistsPort {

    private final CourseClient courseClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CourseRestAdapter(CourseClient courseClient, CircuitBreakerFactory<?, ?> circuitBreakerFactory
    ) {
        this.courseClient = courseClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public boolean existsById(CourseId courseId) {

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("courseService");

        return circuitBreaker.run(() -> courseClient.courseExists(courseId.value())        );
    }
}