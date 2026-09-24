package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.EnrollmentMetricsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.UUID;

public class CreateEnrollmentService implements CreateEnrollmentUseCase {

    private final CourseExistsPort courseExistsPort;
    private final SaveEnrollmentPort saveEnrollmentPort;
    private final EnrollmentMetricsPort enrollmentMetricsPort;

    public CreateEnrollmentService(
            CourseExistsPort courseExistsPort,
            SaveEnrollmentPort saveEnrollmentPort,
            EnrollmentMetricsPort enrollmentMetricsPort
    ) {
        this.courseExistsPort = courseExistsPort;
        this.saveEnrollmentPort = saveEnrollmentPort;
        this.enrollmentMetricsPort = enrollmentMetricsPort;
    }

    @Override
    public Enrollment createEnrollment(CreateEnrollmentCommand command) {

        enrollmentMetricsPort.enrollmentCreationAttempted();

        CourseId courseId = new CourseId(command.courseId());

        if (!courseExistsPort.existsById(courseId)) {
            enrollmentMetricsPort.courseValidationFailed();
            throw new CourseNotFoundException(courseId.value());
        }

        Enrollment enrollment =
                new Enrollment(
                        new EnrollmentId(UUID.randomUUID().toString()),
                        courseId,
                        new StudentName(command.studentName())
                );

        Enrollment saved = saveEnrollmentPort.save(enrollment);
        if (saved.getId() != null) {
            enrollmentMetricsPort.enrollmentCreated();
        }
        return saved;
    }
}