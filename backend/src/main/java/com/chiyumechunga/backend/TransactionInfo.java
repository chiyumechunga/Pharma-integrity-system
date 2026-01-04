package com.chiyumechunga.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionInfo(
        String id,
        String type
) {}

