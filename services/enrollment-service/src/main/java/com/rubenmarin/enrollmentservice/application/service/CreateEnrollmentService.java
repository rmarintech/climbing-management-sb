package com.rubenmarin.enrollmentservice.application.service;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;

import java.util.UUID;

public class CreateEnrollmentService implements CreateEnrollmentUseCase {

    private final CourseExistsPort courseExistsPort;
    private final SaveEnrollmentPort saveEnrollmentPort;

    public CreateEnrollmentService(
            CourseExistsPort courseExistsPort,
            SaveEnrollmentPort saveEnrollmentPort
    ) {
        this.courseExistsPort = courseExistsPort;
        this.saveEnrollmentPort = saveEnrollmentPort;
    }

    @Override
    public Enrollment createEnrollment(CreateEnrollmentCommand command) {

        CourseId courseId = new CourseId(command.courseId());

        if (!courseExistsPort.existsById(courseId)) {
            //throw new IllegalArgumentException("Course does not exist");
            throw new CourseNotFoundException(courseId.value());
        }

        Enrollment enrollment =
                new Enrollment(
                        new EnrollmentId(UUID.randomUUID().toString()),
                        courseId,
                        new StudentName(command.studentName())
                );

        return saveEnrollmentPort.save(enrollment);
    }
}