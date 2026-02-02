package com.chiyumechunga.backend.dto;

import java.math.BigDecimal;

public record DashboardStatsDto(
        Long authenticBatches,
        Long pendingBatches,
        Long failedBatches,
        Long totalScans,
        Long suspiciousScans,
        java.math.BigDecimal authenticityRate, // Update this type
        Long totalTransfers,
        BigDecimal avgLatency
) {}