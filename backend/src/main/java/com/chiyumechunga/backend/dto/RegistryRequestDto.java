package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.Future;
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
 *
 * Caller supplies only the fields needed to create a batch:
 *   product_id
 *   batch_number
 *   manufacturer_id
 *   manufacturing_date
 *   expiry_date
 *
 * System/database managed values are deliberately excluded:
 *   registry_id
 *   qr_hash
 *   firefly_id
 *   blockchain_tx_id
 *   current_status
 *   confirmed_at
 *   created_at
 *   product_name (resolved from product_master)
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
        LocalDate expiryDate

) {}