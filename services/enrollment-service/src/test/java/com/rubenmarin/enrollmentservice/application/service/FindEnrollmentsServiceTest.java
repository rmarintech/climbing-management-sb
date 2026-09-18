package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.out.FindEnrollmentsPort;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FindEnrollmentsServiceTest {

    @Test
    void shouldReturnEnrollmentsFromPort() {

        Enrollment enrollment =
                Enrollment.rehydrate(
                        new EnrollmentId("enrollment-1"),
                        new CourseId(10L),
                        new StudentName("Rubén"),
                        EnrollmentStatus.CONFIRMED
                );

        // This line works because FindEnrollmentsPort has one abstract method:
        // List<Enrollment> findAll();
        //
        // So the lambda means:
        //
        // when findAll() is called
        //          ↓
        //  return this list

        FindEnrollmentsPort findEnrollmentsPort = () -> List.of(enrollment);

        FindEnrollmentsService service = new FindEnrollmentsService(findEnrollmentsPort);

        List<Enrollment> result = service.findAll();
        Enrollment resultEnrollment = result.getFirst();

        assertEquals(1, result.size());
        assertEquals("enrollment-1", resultEnrollment.getId().value());
        assertEquals(10L, resultEnrollment.getCourseId().value());
        assertEquals("Rubén", resultEnrollment.getStudentName().value());
        assertEquals(EnrollmentStatus.CONFIRMED, resultEnrollment.getStatus());
    }
}