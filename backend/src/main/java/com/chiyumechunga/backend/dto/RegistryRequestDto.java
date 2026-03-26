package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for incoming batch registration requests.
 * Uses Jakarta Validation to enforce business rules at the API entry point.
 */
public record RegistryRequestDto(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        String productName,

        @NotBlank(message = "Batch number is required")
        @Size(max = 100, message = "Batch number cannot exceed 100 characters")
        String batchNumber,

        @NotNull(message = "Manufacturer ID is required")
        UUID manufacturerId,

        @PastOrPresent(message = "Manufacturing date cannot be in the future")
        LocalDate manufacturingDate,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate

) {}