package com.rubenmarin.enrollmentservice.adapter.out.persistence.mongodb;

import com.rubenmarin.enrollmentservice.application.port.out.FindEnrollmentsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class MongoEnrollmentAdapter implements SaveEnrollmentPort, FindEnrollmentsPort {


    private final EnrollmentRepository enrollmentRepository;

    public MongoEnrollmentAdapter(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Enrollment save(Enrollment enrollment) {

        enrollmentRepository.save(toEnrollmentDocument(enrollment));

        return enrollment;
    }

    @Override
    public List<Enrollment> findAll() {

        return enrollmentRepository.findAll().stream().map(this::toEnrollment).toList();

    }

    private Enrollment toEnrollment(EnrollmentDocument enrollmentDocument) {

        return Enrollment.rehydrate(
                new EnrollmentId(enrollmentDocument.getId()),
                new CourseId(enrollmentDocument.getCourseId()),
                new StudentName(enrollmentDocument.getStudentName()),
                EnrollmentStatus.valueOf(enrollmentDocument.getStatus())
        );
    }


    private EnrollmentDocument toEnrollmentDocument(Enrollment enrollment) {
        return new EnrollmentDocument(
                enrollment.getId().value(),
                enrollment.getCourseId().value(),
                enrollment.getStudentName().value(),
                enrollment.getStatus().name()
        );
    }
}