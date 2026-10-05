package com.nashtech.employeeredisapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeNotFound(
        EmployeeNotFoundException exception,
        HttpServletRequest request
    ) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
    String details = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining("; "));
    String message = details.isEmpty() ? "Validation failed" : "Validation failed: " + details;
    return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
    return error(HttpStatus.BAD_REQUEST, "Validation failed", request);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request
    ) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
    LOGGER.error("Unexpected error while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
    ApiErrorResponse response = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        message,
        request.getRequestURI()
    );
    return ResponseEntity.status(status).body(response);
    }
}