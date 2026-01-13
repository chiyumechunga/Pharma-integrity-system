package com.chiyumechunga.backend.dto.analytics;

public record SuspiciousScanDto(
        String qrHash,
        long scanCount,         // How many times it was scanned (e.g., 50+)
        String riskReason,      // e.g., "Multiple Locations Detected"
        String lastScannedAt    // Timestamp string
) {}