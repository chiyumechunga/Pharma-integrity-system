package com.chiyumechunga.backend.dto.firefly;

import com.chiyumechunga.backend.TransactionInfo; // Ensure this matches your package
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyEventDto(
        UUID id,
        String sequence, // <--- ADD THIS FIELD (The Fix)
        String type,
        String namespace,
        @JsonProperty("tx") TransactionInfo transaction,

        // THE FIX: Parse the output directly into our unified payload model
        //AssetData output,
        // === THE FIX: Added the missing blockchainEvent metadata field ===
        @JsonProperty("blockchainEvent")
        BlockchainEventMetadata blockchainEvent
) {
    // Nested record to capture the name of the smart contract event
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlockchainEventMetadata(
            String id,
            String name,
            AssetData output
    ) {}
}