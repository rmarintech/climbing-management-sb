package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.EnrollmentMetricsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.domain.model.*;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;


// Pure unit test using Mockito mocks for outbound ports.
@ExtendWith(MockitoExtension.class)
class CreateEnrollmentServiceMockitoTest {

    //@Mock:  Mockito creates fake implementations of the outbound ports
    @Mock
    private CourseExistsPort courseExistsPort;

    @Mock
    private SaveEnrollmentPort saveEnrollmentPort;

    @Mock
    private EnrollmentMetricsPort enrollmentMetricsPort;

    // Mockito creates CreateEnrollmentService
    // and injects those mock's into its constructor
    @InjectMocks
    private CreateEnrollmentService createEnrollmentService;


    @Test
    void shouldCreateEnrollmentWhenCourseExists() {

        CreateEnrollmentCommand createEnrollmentCommand =
                new CreateEnrollmentCommand(
                        10L,
                        "Rubén"
                );


        // Stubbing
        Mockito.when(courseExistsPort.existsById(new CourseId(10L))).thenReturn(true);

        // STUB:
        // Simulate persistence by returning the same Enrollment
        // received by the port.
        Mockito.when(saveEnrollmentPort.save(any(Enrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACTION:
        Enrollment created =
                createEnrollmentService.createEnrollment(createEnrollmentCommand);

        // Assertions
        Assertions.assertEquals(new CourseId(10L), created.getCourseId());
        Assertions.assertEquals(new StudentName("Rubén"), created.getStudentName());
        Assertions.assertEquals(EnrollmentStatus.PENDING, created.getStatus());

        // Verify
        Mockito.verify(courseExistsPort).existsById(new CourseId(10L));

        //Verify saved Object
        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        Mockito.verify(saveEnrollmentPort).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();

        Assertions.assertEquals(EnrollmentStatus.PENDING, savedEnrollment.getStatus());
        Assertions.assertEquals(new StudentName("Rubén"), savedEnrollment.getStudentName());
        Assertions.assertEquals(new CourseId(10L), savedEnrollment.getCourseId());

        Mockito.verify(enrollmentMetricsPort).enrollmentCreationAttempted();
        Mockito.verify(enrollmentMetricsPort).enrollmentCreated();
        Mockito.verify(enrollmentMetricsPort, Mockito.never()).courseValidationFailed();

    }

    @Test
    void shouldNotCreateEnrollmentWhenCourseDoesNotExist() {

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        999L,
                        "Rubén"
                );
        // Stubbing
        Mockito.when(courseExistsPort.existsById(new CourseId(999L))).thenReturn(false);

        CourseNotFoundException exception = Assertions.assertThrows(
                CourseNotFoundException.class,
                () -> createEnrollmentService.createEnrollment(command)
        );

        //Assertions
        Assertions.assertEquals("Course not found: 999", exception.getMessage());

        //Verify
        Mockito.verify(courseExistsPort).existsById(new CourseId(999L));
        // Course validation fails -> exception thrown -> persistence is NOT called
        Mockito.verify(saveEnrollmentPort, Mockito.never()).save(any(Enrollment.class));


        Mockito.verify(enrollmentMetricsPort).enrollmentCreationAttempted();
        Mockito.verify(enrollmentMetricsPort).courseValidationFailed();
        Mockito.verify(enrollmentMetricsPort, Mockito.never()).enrollmentCreated();
    }

    @Test
    void shouldNotRecordEnrollmentAsCreatedWhenPersistenceFails() {

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        10L,
                        "Rubén"
                );

        Mockito.when(courseExistsPort.existsById(new CourseId(10L))).thenReturn(true);

        Mockito.when(
                saveEnrollmentPort.save(any(Enrollment.class))
        ).thenThrow(new RuntimeException("MongoDB unavailable"));

        Assertions.assertThrows(
                RuntimeException.class,
                () -> createEnrollmentService.createEnrollment(command)
        );

        Mockito.verify(enrollmentMetricsPort).enrollmentCreationAttempted();
        Mockito.verify(enrollmentMetricsPort, Mockito.never()).courseValidationFailed();
        Mockito.verify(enrollmentMetricsPort, Mockito.never()).enrollmentCreated();
    }
}