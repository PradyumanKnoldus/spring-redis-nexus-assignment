package com.nashtech.employeeredisapp.exception;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path
) {
}