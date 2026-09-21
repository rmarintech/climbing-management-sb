package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
//Pure unit test for CreateEnrollmentService using fake/mock ports.
class CreateEnrollmentServiceTest {

    @Test
    void shouldCreateEnrollmentWhenCourseExists() {

        // Fake outbound port:
        // pretend that the remote Course Service says the Course exists.
        CourseExistsPort courseExistsPort = courseId -> true;

        // Fake persistence adapter:
        // return exactly the Enrollment that the application asks us to save.
        SaveEnrollmentPort saveEnrollmentPort = enrollment -> enrollment;

        CreateEnrollmentService createEnrollmentService =
                new CreateEnrollmentService(
                        courseExistsPort,
                        saveEnrollmentPort
                );

        CreateEnrollmentCommand createEnrollmentCommand =
                new CreateEnrollmentCommand(
                        10L,
                        "Rubén"
                );

        Enrollment createEnrollmentResult = createEnrollmentService.createEnrollment(createEnrollmentCommand);

        assertNotNull(createEnrollmentResult);
        assertNotNull(createEnrollmentResult.getId());
        assertEquals(10L, createEnrollmentResult.getCourseId().value());
        assertEquals("Rubén", createEnrollmentResult.getStudentName().value());
        assertEquals(EnrollmentStatus.PENDING, createEnrollmentResult.getStatus());
    }

    @Test
    void shouldNotCreateEnrollmentWhenCourseDoesNotExist() {

        CourseExistsPort courseExistsPort = courseId -> false;

        //If CreateEnrollmentService accidentally calls persistence
        // after Course validation fails, the test fails immediately.
        SaveEnrollmentPort saveEnrollmentPort =
                enrollment -> {
                    fail("Enrollment should not be saved");
                    return enrollment;
                };

        CreateEnrollmentService createEnrollmentService =
                new CreateEnrollmentService(
                        courseExistsPort,
                        saveEnrollmentPort
                );

        CreateEnrollmentCommand createEnrollmentCommand =
                new CreateEnrollmentCommand(
                        999L,
                        "Rubén"
                );

        // “Create a little function that takes no parameters and,
        // when executed, runs createEnrollment(...).”
        //  method call= do it now
        //  () -> methodCall() = give me a function that can do it later


        CourseNotFoundException exception =
                assertThrows(
                        CourseNotFoundException.class,
                        () -> createEnrollmentService.createEnrollment(createEnrollmentCommand)
                );


        assertEquals("Course not found: " + 999L, exception.getMessage());
    }
}