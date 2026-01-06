package com.chiyumechunga.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized structure for all API error responses.
 * This ensures the Frontend always knows how to parse errors.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,      // When the error happened
        int status,                   // HTTP Status Code (e.g., 400, 404, 500)
        String error,                 // Short error type (e.g., "Bad Request")
        String message,               // User-friendly message (e.g., "Product Name cannot be empty")
        String path,                  // The API endpoint that failed (e.g., "/api/v1/registry")
        Map<String, String> validationErrors // Optional: Specific field errors (e.g., "batchNumber": "too long")
) {}