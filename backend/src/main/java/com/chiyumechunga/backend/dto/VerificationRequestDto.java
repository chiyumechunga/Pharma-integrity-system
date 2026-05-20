package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequestDto(
        @NotBlank(message = "QR data cannot be blank")
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid QR Hash format")
         String qrHash,

        @NotBlank(message = "Device Fingerprint is required")
        String deviceFingerprint, // Important for "Anti-Counterfeit" analytics

        @NotBlank(message = "GPS Coordinates are required")
        String geoLocation        // Important for "Supply Chain Mapping"
) {}