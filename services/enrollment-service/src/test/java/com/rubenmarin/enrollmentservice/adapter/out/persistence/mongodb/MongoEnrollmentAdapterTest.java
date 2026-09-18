package com.rubenmarin.enrollmentservice.adapter.out.persistence.mongodb;

import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// This test answers one very specific question:
// Does MongoEnrollmentAdapter correctly translate our domain Enrollment
// into an EnrollmentDocument before calling Mongo persistence?


class MongoEnrollmentAdapterTest {

    @Test
    void shouldMapDomainEnrollmentToMongoDocument() {

        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);

        MongoEnrollmentAdapter mongoEnrollmentAdapter = new MongoEnrollmentAdapter(enrollmentRepository);

        Enrollment enrollment =
                new Enrollment(
                        new EnrollmentId("enrollment-1"),
                        new CourseId(10L),
                        new StudentName("Rubén")
                );

        Enrollment enrollmentResult = mongoEnrollmentAdapter.save(enrollment);

        // "Mockito, capture the object that was passed
        // to enrollmentRepository.save(...),
        // because I want to inspect it."

        ArgumentCaptor<EnrollmentDocument> captor =
                ArgumentCaptor.forClass(
                        EnrollmentDocument.class
                );

        // It does two things:
        //       verify(...)
        //          ↓
        //      prove save() was actually called
        //           &
        //      captor.capture()
        //          ↓
        //      grab the EnrollmentDocument
        //      that was passed into save()

        verify(enrollmentRepository).save(captor.capture());


        EnrollmentDocument savedDocument = captor.getValue();

        assertEquals("enrollment-1", savedDocument.getId());
        assertEquals(10L, savedDocument.getCourseId());
        assertEquals("Rubén", savedDocument.getStudentName());
        assertEquals(EnrollmentStatus.PENDING.name(), savedDocument.getStatus());
        assertSame(enrollment, enrollmentResult);

        // assertEquals() means: same value?
        // assertSame() means: literally the exact same Java object instance?
    }

    //    This specifically proves:
    //
    //    Mongo "CONFIRMED"
    //            ↓
    //    EnrollmentStatus.CONFIRMED
    //
    //    without accidentally resetting the aggregate to PENDING.
    @Test
    void shouldMapMongoDocumentsToDomainEnrollments() {

        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);

        MongoEnrollmentAdapter mongoEnrollmentAdapter = new MongoEnrollmentAdapter(enrollmentRepository);

        EnrollmentDocument document =
                new EnrollmentDocument(
                        "enrollment-1",
                        10L,
                        "Rubén",
                        "CONFIRMED"
                );

        when(enrollmentRepository.findAll()).thenReturn(List.of(document));

        List<Enrollment> result = mongoEnrollmentAdapter.findAll();

        assertEquals(1, result.size());

        Enrollment enrollment = result.getFirst();

        assertEquals("enrollment-1", enrollment.getId().value());
        assertEquals(10L, enrollment.getCourseId().value());
        assertEquals("Rubén", enrollment.getStudentName().value());
        assertEquals(EnrollmentStatus.CONFIRMED, enrollment.getStatus());
    }
}