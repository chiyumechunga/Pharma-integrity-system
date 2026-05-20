package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getOverview() {
        return ResponseEntity.ok(analyticsService.getDashboardOverview());
    }

    // CHANGED: Return the View Maps directly for the frontend
    @GetMapping("/suspicious-patterns")
    public ResponseEntity<List<Map<String, Object>>> getSecurityAlerts() {
        return ResponseEntity.ok(analyticsService.getSuspiciousPatterns());
    }

    // NEW: Added the missing Verification Trends endpoint
    @GetMapping("/verification-trends")
    public ResponseEntity<List<Map<String, Object>>> getVerificationTrends() {
        return ResponseEntity.ok(analyticsService.getVerificationTrends());
    }

    @GetMapping("/expiry-risks")
    public ResponseEntity<List<ExpiryRiskDto>> getExpiryRisks(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(analyticsService.getExpiryRiskOverview(days));
    }

    @GetMapping("/lab-stats")
    public ResponseEntity<List<LabQualityReportDto>> getQualityStats() {
        return ResponseEntity.ok(analyticsService.getLabQualityStats());
    }
}