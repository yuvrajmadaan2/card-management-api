package com.wizz.card_management.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.OptimisticLockException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        String requestId =
                request.getHeader("X-Request-Id");

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "01",
                        message
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.BAD_REQUEST
                );

        if (requestId != null && !requestId.isBlank()) {
            builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        String message =
                exception.getHeaderName()
                        + " header is required";

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "01",
                        message
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.BAD_REQUEST
                );

        if (requestId != null && !requestId.isBlank()) {
            builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException exception,
            HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "09",
                        exception.getMessage()
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.CONFLICT
                );

        if (requestId != null && !requestId.isBlank()) {
            builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
    }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
                DataIntegrityViolationException exception,
                HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        log.warn(
                "Data integrity violation requestId={}",
                requestId,
                exception
        );

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "99",
                        "Data integrity constraint violation"
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.CONFLICT
                );

        if (requestId != null && !requestId.isBlank()) {
                builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
        }

        @ExceptionHandler(OptimisticLockException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLockException(
                OptimisticLockException exception,
                HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        log.warn(
                "Optimistic locking conflict requestId={}",
                requestId
        );

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "409",
                        "Card has been modified by another request"
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.CONFLICT
                );

        if (requestId != null && !requestId.isBlank()) {
                builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
        }

        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleObjectOptimisticLockingFailure(
                ObjectOptimisticLockingFailureException exception,
                HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        log.warn(
                "Optimistic locking conflict requestId={}",
                requestId
        );

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "409",
                        "Resource has been modified by another request"
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(HttpStatus.CONFLICT);

        if (requestId != null && !requestId.isBlank()) {
                builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
        }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalException(
            Exception exception,
            HttpServletRequest request) {

        String requestId =
                request.getHeader("X-Request-Id");

        log.error(
                "Unexpected error requestId={}",
                requestId,
                exception
        );

        ErrorResponse response =
                new ErrorResponse(
                        requestId,
                        "99",
                        "Internal server error"
                );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                );

        if (requestId != null && !requestId.isBlank()) {
            builder.header("X-Request-Id", requestId);
        }

        return builder.body(response);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {

        private String referenceId;
        private String responseCode;
        private String responseDesc;

        public ErrorResponse(
                String referenceId,
                String responseCode,
                String responseDesc) {

            this.referenceId = referenceId;
            this.responseCode = responseCode;
            this.responseDesc = responseDesc;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public String getResponseCode() {
            return responseCode;
        }

        public String getResponseDesc() {
            return responseDesc;
        }
    }
}
