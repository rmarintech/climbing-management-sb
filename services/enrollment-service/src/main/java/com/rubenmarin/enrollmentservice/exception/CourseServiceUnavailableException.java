package com.rubenmarin.enrollmentservice.exception;

public class CourseServiceUnavailableException
        extends RuntimeException {

    public CourseServiceUnavailableException(Throwable cause) {
        super("Course Service is currently unavailable", cause);
    }
}
