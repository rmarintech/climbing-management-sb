package com.rubenmarin.enrollmentservice.exception;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCourseNotFound(
            CourseNotFoundException exception) {

        return new ErrorResponse(
                Instant.now(),
                exception.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
    }

    @ExceptionHandler(CourseServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleCourseServiceUnavailableException(
            CourseServiceUnavailableException exception) {

        return new ErrorResponse(
                Instant.now(),
                exception.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        return new ErrorResponse(
                Instant.now(),
                exception.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
    }
}