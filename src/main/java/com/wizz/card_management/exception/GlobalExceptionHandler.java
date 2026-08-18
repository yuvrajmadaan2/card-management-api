package com.wizz.card_management.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        String requestId = request.getHeader("X-Request-Id");

        ErrorResponse response = new ErrorResponse(
                requestId,
                "01",
                message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Request-Id", requestId)
                .body(response);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException exception,
            HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-Id");

        ErrorResponse response = new ErrorResponse(
                requestId,
                "09",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("X-Request-Id", requestId)
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-Id");

        ErrorResponse response = new ErrorResponse(
                requestId,
                "09",
                "Idempotency key conflict"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("X-Request-Id", requestId)
                .body(response);
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