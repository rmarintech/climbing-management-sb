package com.rubenmarin.enrollmentservice.domain.model;


// Deliberately using a class because a DDD Entity is defined primarily by its identity,
// not simply by all its values.

//Enrollment is an Entity because two enrollments can contain the same data but still represent different real things.
//Enrollment A
//id = "abc"
//courseId = 10
//studentName = "Rubén"
//
//Enrollment B
//id = "xyz"
//courseId = 10
//studentName = "Rubén"
//Enrollment
//    ↓
//Entity
//    ↓
//class
//
//Value Object, on the other hand, is defined entirely by its value. CourseId(10)
//CourseId
//    ↓
//Value Object
//    ↓
//record

//DDD encourages us to make concepts explicit:
//CourseId
//        StudentId
//EnrollmentId


public class Enrollment {

    private final EnrollmentId id;
    private final CourseId courseId;
    private final StudentName studentName;
    private EnrollmentStatus status;

    public Enrollment(
            EnrollmentId id,
            CourseId courseId,
            StudentName studentName
    ) {
        this.id = id;
        this.courseId = courseId;
        this.studentName = studentName;
        this.status = EnrollmentStatus.PENDING;
    }

    public EnrollmentId getId() {
        return id;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public StudentName getStudentName() {
        return studentName;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void confirm() {
        if (status != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Only a pending enrollment can be confirmed");
        }
        status = EnrollmentStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("Enrollment is already cancelled");
        }
        status = EnrollmentStatus.CANCELLED;
    }
}