package com.rubenmarin.enrollmentservice.adapter.out.persistence.mongodb;

import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.domain.model.*;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

@DataMongoTest
@Testcontainers
@Import(MongoEnrollmentAdapter.class)
class MongoEnrollmentAdapterIntegrationTest {

    @Container //start this Docker container for the test lifecycle.
    @ServiceConnection //Spring Boot, use this container as the MongoDB connection for the application context.
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MongoEnrollmentAdapter mongoEnrollmentAdapter;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void cleanDatabase() {
        enrollmentRepository.deleteAll();
    }

    @Test
    void shouldPersistEnrollmentInMongoDB() {

        //Stub
        Enrollment enrollment =
                new Enrollment(
                        new EnrollmentId("integration-001"),
                        new CourseId(10L),
                        new StudentName("Rubén")
                );


        //Action
        Enrollment saved = mongoEnrollmentAdapter.save(enrollment);


        //Asserts
        Optional<EnrollmentDocument> persisted = enrollmentRepository.findById("integration-001");

        Assertions.assertTrue(persisted.isPresent());
        Assertions.assertEquals(10L, persisted.get().getCourseId());
        Assertions.assertEquals("Rubén", persisted.get().getStudentName());
        Assertions.assertEquals(EnrollmentStatus.PENDING.name(), persisted.get().getStatus());


    }

    @Test
    void shouldFindEnrollmentsFromMongoDB() {

        String id = "integration-001";
        Long courseId = 10L;
        String studentName = "Rubén";
        String status = EnrollmentStatus.PENDING.name();
        EnrollmentDocument document = new EnrollmentDocument(id, courseId, studentName, status);

        EnrollmentDocument saved = enrollmentRepository.save(document);

        List<Enrollment> enrollmentList = mongoEnrollmentAdapter.findAll();

        Assertions.assertEquals(1, enrollmentList.size());

        Enrollment enrollment = enrollmentList.getFirst();
        Assertions.assertEquals(new EnrollmentId(id), enrollment.getId());
        Assertions.assertEquals(new CourseId(courseId), enrollment.getCourseId());
        Assertions.assertEquals(new StudentName(studentName), enrollment.getStudentName());
        Assertions.assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());

    }
}