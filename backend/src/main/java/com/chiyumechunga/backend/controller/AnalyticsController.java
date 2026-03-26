package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.dto.analytics.SuspiciousScanDto;
import com.chiyumechunga.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. DASHBOARD OVERVIEW (Renamed from /overview to /dashboard)
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getOverview() {
        return ResponseEntity.ok(analyticsService.getDashboardOverview());
    }

    // 2. SUSPICIOUS PATTERNS (Renamed from /security/alerts)
    @GetMapping("/suspicious-patterns")
    public ResponseEntity<List<SuspiciousScanDto>> getSecurityAlerts() {
        return ResponseEntity.ok(analyticsService.getSuspiciousScanAlerts());
    }

    // 3. SUPPLY CHAIN REPORTS
    @GetMapping("/expiry-risks")
    public ResponseEntity<List<ExpiryRiskDto>> getExpiryRisks(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(analyticsService.getExpiryRiskOverview(days));
    }

    // 4. QUALITY REPORTS
    @GetMapping("/lab-stats")
    public ResponseEntity<List<LabQualityReportDto>> getQualityStats() {
        return ResponseEntity.ok(analyticsService.getLabQualityStats());
    }
}