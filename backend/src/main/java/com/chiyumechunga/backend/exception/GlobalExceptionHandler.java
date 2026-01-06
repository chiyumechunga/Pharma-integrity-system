package com.chiyumechunga.backend.exception;

import com.chiyumechunga.backend.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Validation Errors (e.g., @NotBlank, @Future violations)
     * Securely sanitizes the output to prevent Reflected XSS.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // 1. Sanitize Validation Messages
        // The error message might contain the bad input, so we escape it.
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String safeField = HtmlUtils.htmlEscape(error.getField());
            String safeMessage = HtmlUtils.htmlEscape(error.getDefaultMessage());
            errors.put(safeField, safeMessage);
        }

        log.warn("Validation failed for request to {}", request.getRequestURI());

        // 2. Create Response with Sanitized Path
        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input data contains errors",
                HtmlUtils.htmlEscape(request.getRequestURI()), // <--- FIX: Sanitize the Path
                errors
        );

        return buildResponse(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Resource Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HtmlUtils.htmlEscape(ex.getMessage()), // Sanitize the message just in case
                HtmlUtils.htmlEscape(request.getRequestURI()), // <--- FIX: Sanitize the Path
                null
        );

        return buildResponse(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle Generic/Unexpected Errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact support.",
                HtmlUtils.htmlEscape(request.getRequestURI()), // <--- FIX: Sanitize the Path
                null
        );

        return buildResponse(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper to add Security Headers to all Error Responses
     */
    private ResponseEntity<ErrorResponseDto> buildResponse(ErrorResponseDto body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(body, headers, status);
    }
}