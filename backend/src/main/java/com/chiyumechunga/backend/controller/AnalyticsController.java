package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.dto.analytics.SuspiciousScanDto;
import com.chiyumechunga.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor; // Handles Constructor Injection automatically
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor // <--- Generates the constructor for 'final' fields
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // --- 1. EXECUTIVE OVERVIEW ---
    // This delegates to the Service, which handles the Repository counts.
    @GetMapping("/overview")
    public ResponseEntity<DashboardStatsDto> getOverview() {
        return ResponseEntity.ok(analyticsService.getDashboardOverview());
    }

    // --- 2. SECURITY REPORTS ---
    @GetMapping("/security/alerts")
    public ResponseEntity<List<SuspiciousScanDto>> getSecurityAlerts() {
        return ResponseEntity.ok(analyticsService.getSuspiciousScanAlerts());
    }

    // --- 3. SUPPLY CHAIN REPORTS ---
    @GetMapping("/supply-chain/expiry-risk")
    public ResponseEntity<List<ExpiryRiskDto>> getExpiryRisks(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(analyticsService.getExpiryRiskOverview(days));
    }

    // --- 4. QUALITY REPORTS ---
    @GetMapping("/quality/lab-stats")
    public ResponseEntity<List<LabQualityReportDto>> getQualityStats() {
        return ResponseEntity.ok(analyticsService.getLabQualityStats());
    }
}