package com.rubenmarin.enrollmentservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


//tests the entity business behavior

class EnrollmentTest {

    @Test
    void newEnrollmentShouldStartAsPending() {

        Enrollment enrollment = new Enrollment(
                new EnrollmentId("enrollment-1"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());
    }

    @Test
    void pendingEnrollmentShouldBeConfirmed() {

        Enrollment enrollment = new Enrollment(
                new EnrollmentId("enrollment-1"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        enrollment.confirm();

        assertEquals(EnrollmentStatus.CONFIRMED, enrollment.getStatus());
    }

    @Test
    void confirmedEnrollmentCannotBeConfirmedAgain() {

        Enrollment enrollment = new Enrollment(
                new EnrollmentId("enrollment-1"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        enrollment.confirm();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        enrollment::confirm
                );

        assertEquals("Only a pending enrollment can be confirmed", exception.getMessage());
    }

    @Test
    void cancelledEnrollmentCannotBeConfirmed() {

        Enrollment enrollment = new Enrollment(
                new EnrollmentId("enrollment-1"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        enrollment.cancel();

        assertThrows(IllegalStateException.class, enrollment::confirm);
    }

    @Test
    void cancelledEnrollmentCannotBeCancelledAgain() {

        Enrollment enrollment = new Enrollment(
                new EnrollmentId("enrollment-1"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        enrollment.cancel();

        assertThrows(IllegalStateException.class, enrollment::cancel);
    }

    @Test
    void studentNameCannotBeBlank() {

        assertThrows(IllegalArgumentException.class, () -> new StudentName(" "));
    }

    @Test
    void studentNameShouldBeTrimmed() {

        StudentName studentName = new StudentName("  Rubén  ");

        assertEquals("Rubén", studentName.value());
    }
}