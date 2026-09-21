package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.out.FindEnrollmentsPort;
import com.rubenmarin.enrollmentservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

// Pure unit test using Mockito mocks for outbound ports.
@ExtendWith(MockitoExtension.class)
public class FindEnrollmentsServiceMockitoTest {

    @Mock
    private FindEnrollmentsPort findEnrollmentsPort;

    @InjectMocks
    private FindEnrollmentsService findEnrollmentsService;


    @Test
    void shouldReturnEnrollmentsWhenEnrollmentsExist() {

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(
                new Enrollment(
                        new EnrollmentId("xxx"),
                        new CourseId(10L),
                        new StudentName("Rubén")
                )
        );
        //Stub.

        Mockito.when(findEnrollmentsPort.findAll()).thenReturn(enrollments);
        //Action
        List<Enrollment> enrollmentList = findEnrollmentsService.findAll();
        // Assert
        Assertions.assertNotNull(enrollmentList);
        Assertions.assertEquals(enrollments.size(), enrollmentList.size());
        Assertions.assertEquals(new EnrollmentId("xxx"), enrollmentList.getFirst().getId());
        Assertions.assertEquals(new StudentName("Rubén"), enrollmentList.getFirst().getStudentName());
        Assertions.assertEquals(new CourseId(10L), enrollmentList.getFirst().getCourseId());
        Assertions.assertEquals(EnrollmentStatus.PENDING, enrollmentList.getFirst().getStatus());
        //Verify
        Mockito.verify(findEnrollmentsPort, Mockito.times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoEnrollmentsExist() {

        //Stub.
        Mockito.when(findEnrollmentsPort.findAll()).thenReturn(List.of());
        //Action
        List<Enrollment> enrollmentList = findEnrollmentsService.findAll();
        // Assert
        Assertions.assertNotNull(enrollmentList);
        Assertions.assertTrue(enrollmentList.isEmpty());

        //Verify
        Mockito.verify(findEnrollmentsPort, Mockito.times(1)).findAll();
    }

}
