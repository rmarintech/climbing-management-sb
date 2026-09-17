package com.rubenmarin.enrollmentservice.adapter.out.persistence.mongodb;

import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Component;




@Component
public class MongoEnrollmentAdapter implements SaveEnrollmentPort {

    private final EnrollmentRepository enrollmentRepository;

    public MongoEnrollmentAdapter(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Enrollment save(Enrollment enrollment) {

        EnrollmentDocument document =
                new EnrollmentDocument(
                        enrollment.getId().value(),
                        enrollment.getCourseId().value(),
                        enrollment.getStudentName().value(),
                        enrollment.getStatus().name()
                );

        enrollmentRepository.save(document);

        return enrollment;
    }
}