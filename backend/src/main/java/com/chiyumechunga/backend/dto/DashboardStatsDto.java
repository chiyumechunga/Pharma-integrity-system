package com.chiyumechunga.backend.dto;

public record DashboardStatsDto(
        long totalRegisteredBatches,
        long totalParticipants,
        long failedScans,   // Critical: Potential counterfeits
        long successfulScans
) {}