package com.winter.orderprocessing.controller;

import com.winter.orderprocessing.dto.ApiError;
import com.winter.orderprocessing.exception.InvalidOrderStatusException;
import com.winter.orderprocessing.exception.InvalidOrderTransitionException;
import com.winter.orderprocessing.exception.OrderNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(OrderNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidOrderStatusException.class, InvalidOrderTransitionException.class})
    public ResponseEntity<ApiError> handleInvalidOrderRequest(RuntimeException exception,
                                                               HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Request validation failed.");
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "The order changed while it was being updated. Reload and retry.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected API error for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
