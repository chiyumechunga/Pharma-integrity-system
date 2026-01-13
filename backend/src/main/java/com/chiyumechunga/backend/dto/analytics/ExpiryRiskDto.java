package com.chiyumechunga.backend.dto.analytics;

import java.time.LocalDate;

public record ExpiryRiskDto(
        String productName,
        String batchNumber,
        LocalDate expiryDate,
        String currentOwner, // e.g., "ZAMMSA Warehouse 1"
        long daysRemaining   // e.g., 45 (Computed field)
) {}