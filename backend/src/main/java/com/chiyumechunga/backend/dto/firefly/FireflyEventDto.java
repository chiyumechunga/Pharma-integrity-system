package com.chiyumechunga.backend.dto.firefly;

import com.chiyumechunga.backend.TransactionInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyEventDto(
        String id,
        String type,   // <--- required for event.type() to work
        String namespace,
        @JsonProperty("tx") TransactionInfo transaction,
        EventOutput output
) {}


