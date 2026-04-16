package com.chiyumechunga.backend.dto.firefly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;

/**
 * LOGIC: Data Mapping
 * This record defines the unified business data structure that the
 * Fabric Chaincode emits for both 'AssetCreated' and 'CustodyTransferred' events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetData(

        // --- ASSET CREATED FIELDS ---
        @JsonProperty("qrHash")
        String qrHash,

        @JsonProperty("productName")
        String productName,

        @JsonProperty("batchNumber")
        String batchNumber,

        @JsonProperty("manufacturerId")
        UUID manufacturerId,

        @JsonProperty("expiryDate")
        LocalDate expiryDate,

        @JsonProperty("currentStatus")
        String currentStatus,

        @JsonProperty("productId")
        String productId,

        @JsonProperty("requiresColdChain")
        Boolean requiresColdChain,

        @JsonProperty("approvedByZamra")
        Boolean approvedByZamra,

        // --- CUSTODY TRANSFERRED FIELDS ---
        @JsonProperty("fromParticipantId")
        UUID fromParticipantId,

        @JsonProperty("toParticipantId")
        UUID toParticipantId,

        @JsonProperty("eventType")
        String eventType,

        @JsonProperty("quantity")
        Integer quantity,

        @JsonProperty("txId")
        String txId
) {}