package com.chiyumechunga.backend.exception;

import com.chiyumechunga.backend.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ── Spring MVC built-in overrides ─────────────────────────────────────

    /**
     * Handles 405 Method Not Allowed, 400 Bad Request (malformed JSON),
     * 415 Unsupported Media Type, and other Spring MVC exceptions automatically.
     * We override to wrap the response in our ErrorResponseDto format.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest webRequest) {

        String path = ((ServletWebRequest) webRequest).getRequest().getRequestURI();
        int status = statusCode.value();

        String message = switch (status) {
            case 405 -> "HTTP method not supported for this endpoint.";
            case 400 -> "Request body is missing or contains invalid JSON.";
            case 415 -> "Content-Type must be application/json.";
            case 404 -> "The requested endpoint does not exist.";
            default  -> "Request could not be processed.";
        };

        log.warn("Spring MVC exception [{}] at {}: {}", status, path, ex.getMessage());

        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(), status,
                HttpStatus.resolve(status) != null
                        ? HttpStatus.resolve(status).getReasonPhrase() : "Error",
                message,
                HtmlUtils.htmlEscape(path), null
        );

        return new ResponseEntity<>(response, headers, statusCode);
    }

    /**
     * Validation errors (@Valid / @NotBlank etc.)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest webRequest) {

        String path = ((ServletWebRequest) webRequest).getRequest().getRequestURI();
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    HtmlUtils.htmlEscape(error.getField()),
                    HtmlUtils.htmlEscape(error.getDefaultMessage())
            );
        }

        log.warn("Validation failed at {}", path);

        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(), 400, "Validation Failed",
                "Input data contains errors.",
                HtmlUtils.htmlEscape(path), errors
        );

        return new ResponseEntity<>(response, headers, HttpStatus.BAD_REQUEST);
    }

    // ── Application-specific handlers ─────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Failed login attempt at {}", request.getRequestURI());
        return buildResponse(new ErrorResponseDto(
                LocalDateTime.now(), 401, "Unauthorized",
                "Invalid email or password.",   // intentionally vague
                HtmlUtils.htmlEscape(request.getRequestURI()), null
        ), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for '{}' at {}", request.getRemoteUser(), request.getRequestURI());
        return buildResponse(new ErrorResponseDto(
                LocalDateTime.now(), 403, "Forbidden",
                "You do not have permission to access this resource.",
                HtmlUtils.htmlEscape(request.getRequestURI()), null
        ), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(new ErrorResponseDto(
                LocalDateTime.now(), 404, "Not Found",
                HtmlUtils.htmlEscape(ex.getMessage()),
                HtmlUtils.htmlEscape(request.getRequestURI()), null
        ), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildResponse(new ErrorResponseDto(
                LocalDateTime.now(), 409, "Conflict",
                HtmlUtils.htmlEscape(ex.getMessage()),
                HtmlUtils.htmlEscape(request.getRequestURI()), null
        ), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return buildResponse(new ErrorResponseDto(
                LocalDateTime.now(), 500, "Internal Server Error",
                "An unexpected error occurred. Please contact support.",
                HtmlUtils.htmlEscape(request.getRequestURI()), null
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Shared helper ──────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponseDto> buildResponse(ErrorResponseDto body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, status);
    }
}