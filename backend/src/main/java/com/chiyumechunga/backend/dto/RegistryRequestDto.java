package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a batch in pharmaceutical_registry.
 *
 * Important design rule:
 * This DTO no longer contains productName as free text.
 * The product name must come from product_master.generic_name after productId is resolved.
 * That prevents inconsistent naming between the catalog and registered batches.
 */
public record RegistryRequestDto(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotBlank(message = "Batch number is required")
        @Size(max = 100, message = "Batch number cannot exceed 100 characters")
        String batchNumber,

        @NotNull(message = "Manufacturer ID is required")
        UUID manufacturerId,

        @PastOrPresent(message = "Manufacturing date cannot be in the future")
        LocalDate manufacturingDate,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate,

        // NEW: Required to tell the system how many individual bottles to generate
        @NotNull(message = "Batch unit count is required")
        @Min(value = 1, message = "Batch must contain at least 1 unit")
        Integer batchUnitCount

) {}