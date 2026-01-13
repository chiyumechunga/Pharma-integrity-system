package com.chiyumechunga.backend.dto.firefly;

import com.chiyumechunga.backend.TransactionInfo; // Ensure this matches your package
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyEventDto(
        String id,
        String sequence, // <--- ADD THIS FIELD (The Fix)
        String type,
        String namespace,
        @JsonProperty("tx") TransactionInfo transaction,
        EventOutput output
) {}