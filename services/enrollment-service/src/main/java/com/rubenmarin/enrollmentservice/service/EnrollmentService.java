package com.rubenmarin.enrollmentservice.service;

import com.rubenmarin.enrollmentservice.client.CourseClient;
import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;
import com.rubenmarin.enrollmentservice.model.Enrollment;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseClient courseClient) {

        this.enrollmentRepository = enrollmentRepository;
        this.courseClient = courseClient;
    }

    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll().stream().map(this::toModel).toList();
    }

    public Enrollment create(Enrollment enrollment) {

        if (!courseClient.courseExists(enrollment.courseId())) {
            throw new CourseNotFoundException(enrollment.courseId());
        }

        EnrollmentDocument enrollmentDocument = new EnrollmentDocument(
                enrollment.courseId(),
                enrollment.studentName());

        EnrollmentDocument saved = enrollmentRepository.save(enrollmentDocument);

        return toModel(saved);
    }

    private Enrollment toModel(EnrollmentDocument document) {
        return new Enrollment(
                document.getId(),
                document.getCourseId(),
                document.getStudentName()
        );
    }

}