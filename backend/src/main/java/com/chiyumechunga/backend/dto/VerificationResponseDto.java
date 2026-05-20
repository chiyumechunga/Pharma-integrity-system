package com.chiyumechunga.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// @JsonInclude(JsonInclude.Include.NON_NULL) ensures null fields aren't sent to the client
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationResponseDto(
        String productName,// Name of the drug
        String status,            // e.g., "ON_CHAIN", "EXPIRED"
        String blockchainTxId,    // The source of truth hash from Ethereum/Fabric
        boolean isValid,          // Helper flag for Frontend logic (Green/Red)
        String message,           // Human-readable message (e.g., "Product is Authentic")
        String manufacturerName   // Optional: Name of the manufacturer if available
) {}