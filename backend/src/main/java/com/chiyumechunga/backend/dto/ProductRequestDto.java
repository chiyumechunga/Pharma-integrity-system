package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a product in the product_master table.
 *
 * Why this DTO exists separately:
 * - product_master and pharmaceutical_registry are different tables
 * - they receive different values
 * - they have different validation rules
 * - they represent different business actions
 *
 * This DTO maps only to writable columns in product_master:
 *   product_code
 *   generic_name
 *   brand_name
 *   dosage_form
 *   strength
 *   therapeutic_class
 *   requires_cold_chain
 *   approved_by_zamra
 *
 * The following columns are NOT provided by the caller because the database generates them:
 *   product_id  -> UUID primary key
 *   created_at  -> timestamp default current_timestamp
 */
public record ProductRequestDto(

        @NotBlank(message = "Product code is required")
        @Size(max = 50, message = "Product code cannot exceed 50 characters")
        String productCode,

        @NotBlank(message = "Generic name is required")
        @Size(max = 255, message = "Generic name cannot exceed 255 characters")
        String genericName,

        @Size(max = 255, message = "Brand name cannot exceed 255 characters")
        String brandName,

        @Size(max = 100, message = "Dosage form cannot exceed 100 characters")
        String dosageForm,

        @Size(max = 100, message = "Strength cannot exceed 100 characters")
        String strength,

        @Size(max = 100, message = "Therapeutic class cannot exceed 100 characters")
        String therapeuticClass,

        boolean requiresColdChain,

        boolean approvedByZamra

) {}