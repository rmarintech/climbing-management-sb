package com.rubenmarin.enrollmentservice.service;

import com.rubenmarin.enrollmentservice.client.CourseClient;
import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.exception.CourseNotFoundException;
import com.rubenmarin.enrollmentservice.model.Enrollment;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseClient courseClient,
                             CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.enrollmentRepository = enrollmentRepository;
        this.courseClient = courseClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll().stream().map(this::toModel).toList();
    }

    public Enrollment create(Enrollment enrollment) {

        /** CircuitBreaker**/
        // Spring Cloud’s CircuitBreakerFactory.create("...").run(...) API
        // is the standard way to wrap code with the configured circuit breaker
        // The "courseService" name is important because it matches the configuration you created:
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("courseService");
        boolean courseExists = circuitBreaker.run(() -> courseClient.courseExists(enrollment.courseId()));

        if (!courseExists) {
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