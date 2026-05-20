package com.chiyumechunga.backend.dto;

import java.math.BigDecimal;

public record DashboardStatsDto(
        Long authenticBatchesTracked,
        Long pendingConfirmation,
        Long syncFailures,
        Long authenticScans,
        Long flaggedScans,
        BigDecimal authenticityRatePct,
        Long custodyTransfers,
        BigDecimal latestSyncLatencySeconds
) {}