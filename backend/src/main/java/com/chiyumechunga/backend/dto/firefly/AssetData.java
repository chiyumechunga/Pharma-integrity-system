package com.chiyumechunga.backend.dto.firefly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;

/**
 * LOGIC: Data Mapping
 * This record defines the specific business data structure that the
 * Fabric Chaincode emits in the 'AssetCreated' event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetData(

        // Maps JSON "qr_hash" -> Java "qrHash"
        @JsonProperty("qr_hash")
        String qrHash,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("batch_number")
        String batchNumber,

        // We keep this as String here to avoid deserialization errors.
        // The Service layer will convert it to UUID.
        @JsonProperty("manufacturer_id")
        UUID manufacturerId,

        // Jackson will automatically parse ISO-8601 strings (e.g., "2026-12-31") into LocalDate
        @JsonProperty("expiry_date")
        LocalDate expiryDate
) {}