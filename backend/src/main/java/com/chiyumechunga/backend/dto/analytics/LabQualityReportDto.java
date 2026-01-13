package com.chiyumechunga.backend.dto.analytics;

public record LabQualityReportDto(
        String manufacturerName,
        long totalTests,
        long passedTests,
        long failedTests,
        double failureRate // e.g., 5.5 (Percentage)
) {}