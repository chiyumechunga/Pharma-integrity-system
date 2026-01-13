package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.DashboardStatsDto; // <--- Import your existing DTO
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.dto.analytics.SuspiciousScanDto;

import java.util.List;

public interface AnalyticsService {

    // 1. EXECUTIVE SUMMARY (The new method for your DTO)
    DashboardStatsDto getDashboardOverview();

    // 2. DEEP DIVE REPORTS
    List<SuspiciousScanDto> getSuspiciousScanAlerts();
    List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold);
    List<LabQualityReportDto> getLabQualityStats();
}