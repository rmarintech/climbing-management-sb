package com.rubenmarin.enrollmentservice.adapter.out.course.rest;

import com.rubenmarin.enrollmentservice.client.CourseClient;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The test should prove this mapping/delegation:
//
//      CourseId(10L)
//          ↓
//      CourseRestAdapter
//          ↓
//      CircuitBreaker "courseService"
//          ↓
//   CourseClient.courseExists(10L)
//          ↓
//      true / false returned

class CourseRestAdapterTest {

    @Test
    void shouldReturnTrueWhenCourseExists() {

        CourseClient courseClient = mock(CourseClient.class);
        CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("courseService")).thenReturn(circuitBreaker);

        /*
         * The real CircuitBreaker executes the Supplier passed to run(...).
         *
         * Because this CircuitBreaker is mocked, we explicitly tell Mockito
         * to execute that Supplier.
         */
        when(circuitBreaker.run(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);

                    return supplier.get();
                });

        when(courseClient.courseExists(10L)).thenReturn(true);

        CourseRestAdapter courseRestAdapter =
                new CourseRestAdapter(
                        courseClient,
                        circuitBreakerFactory
                );

        boolean result = courseRestAdapter.existsById(new CourseId(10L));

        assertTrue(result);

        verify(circuitBreakerFactory).create("courseService");
        verify(courseClient).courseExists(10L);
    }
}