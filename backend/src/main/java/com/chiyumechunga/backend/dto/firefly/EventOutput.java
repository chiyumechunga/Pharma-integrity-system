package com.chiyumechunga.backend.dto.firefly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventOutput(
        AssetData data
) {}