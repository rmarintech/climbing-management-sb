package com.rubenmarin.enrollmentservice.exception;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rubenmarin.enrollmentservice.api.generated.model.ErrorResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCourseNotFound(
            CourseNotFoundException exception) {

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                exception.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
    }

    @ExceptionHandler(CourseServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleCourseServiceUnavailableException(
            CourseServiceUnavailableException exception) {

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                exception.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                exception.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()
                )
                .orElse("Invalid Request");

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                message,
                HttpStatus.BAD_REQUEST.value()
        );
    }
}