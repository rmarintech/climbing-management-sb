package com.rubenmarin.enrollmentservice.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        String message,
        int status
) {
}