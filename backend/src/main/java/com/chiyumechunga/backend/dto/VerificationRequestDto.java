package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequestDto(
        @NotBlank(message = "QR Hash is required")
        String qrHash,

        @NotBlank(message = "Device Fingerprint is required")
        String deviceFingerprint, // Important for "Anti-Counterfeit" analytics

        @NotBlank(message = "GPS Coordinates are required")
        String geoLocation        // Important for "Supply Chain Mapping"
) {}