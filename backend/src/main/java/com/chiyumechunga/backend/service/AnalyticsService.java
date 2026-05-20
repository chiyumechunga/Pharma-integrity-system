package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {

    // 1. EXECUTIVE SUMMARY
    DashboardStatsDto getDashboardOverview();

    // 2. NETWORK ANALYTICS (Directly pulling from DB Views)
    List<Map<String, Object>> getSuspiciousPatterns();
    List<Map<String, Object>> getVerificationTrends();

    // 3. DEEP DIVE REPORTS
    List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold);
    List<LabQualityReportDto> getLabQualityStats();
}