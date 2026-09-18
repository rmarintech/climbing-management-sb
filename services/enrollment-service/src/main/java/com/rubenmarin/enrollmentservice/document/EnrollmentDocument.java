package com.rubenmarin.enrollmentservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "enrollments")
public class EnrollmentDocument {

    @Id
    private String id;
    private Long courseId;
    private String studentName;
    private String status;

    public EnrollmentDocument() {
    }

    // Constructor used by the new Hexagonal persistence adapter.
    public EnrollmentDocument(
            String id,
            Long courseId,
            String studentName,
            String status
    ) {
        this.id = id;
        this.courseId = courseId;
        this.studentName = studentName;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStatus() {return status;}

}