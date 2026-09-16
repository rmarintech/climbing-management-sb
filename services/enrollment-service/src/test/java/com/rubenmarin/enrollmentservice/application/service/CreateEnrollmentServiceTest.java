package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateEnrollmentServiceTest {

    @Test
    void shouldCreateEnrollmentWhenCourseExists() {

        // Fake outbound port:
        // pretend that the remote Course Service says the Course exists.
        CourseExistsPort courseExistsPort = courseId -> true;

        // Fake persistence adapter:
        // return exactly the Enrollment that the application asks us to save.
        SaveEnrollmentPort saveEnrollmentPort = enrollment -> enrollment;

        CreateEnrollmentService service =
                new CreateEnrollmentService(
                        courseExistsPort,
                        saveEnrollmentPort
                );

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        10L,
                        "Rubén"
                );

        Enrollment result = service.createEnrollment(command);

        assertNotNull(result);

        assertNotNull(result.getId());

        assertEquals(10L, result.getCourseId().value());

        assertEquals("Rubén", result.getStudentName().value());

        assertEquals(EnrollmentStatus.PENDING, result.getStatus());
    }

    @Test
    void shouldNotCreateEnrollmentWhenCourseDoesNotExist() {

        CourseExistsPort courseExistsPort = courseId -> false;

        SaveEnrollmentPort saveEnrollmentPort =
                enrollment -> {
                    fail("Enrollment should not be saved");
                    return enrollment;
                };

        CreateEnrollmentService service =
                new CreateEnrollmentService(
                        courseExistsPort,
                        saveEnrollmentPort
                );

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        999L,
                        "Rubén"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createEnrollment(command)
                );

        assertEquals("Course does not exist", exception.getMessage());
    }
}