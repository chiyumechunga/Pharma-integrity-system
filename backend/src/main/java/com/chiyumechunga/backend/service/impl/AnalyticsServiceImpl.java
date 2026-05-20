package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.repository.AnalyticsRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.service.AnalyticsService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    // 1. Inject the AnalyticsRepository for Dashboard/Trends (using optimized DB Views)
    private final AnalyticsRepository analyticsRepository;

    // 2. Keep the existing repositories needed for Expiry/Quality reports
    private final PharmaceuticalRegistryRepository registryRepo;
    private final RegulatoryScrutinyRepository scrutinyRepo;

    /**
     * 1. EXECUTIVE OVERVIEW (Pulling directly from vw_poc_dashboard view)
     */
    @Override
    public DashboardStatsDto getDashboardOverview() {
        Map<String, Object> data = analyticsRepository.getDashboardStats();

        // Safely map the database view columns to the DTO
        // We use ((Number) ...).longValue() to prevent ClassCastExceptions from Postgres BigInts
        if (data == null || data.isEmpty()) {
            return new DashboardStatsDto(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        return new DashboardStatsDto(
                ((Number) data.getOrDefault("authentic_batches_tracked", 0)).longValue(),
                ((Number) data.getOrDefault("pending_confirmation", 0)).longValue(),
                ((Number) data.getOrDefault("sync_failures", 0)).longValue(),
                ((Number) data.getOrDefault("authentic_scans", 0)).longValue(),
                ((Number) data.getOrDefault("flagged_scans", 0)).longValue(),
                (BigDecimal) data.getOrDefault("authenticity_rate_pct", BigDecimal.ZERO),
                ((Number) data.getOrDefault("custody_transfers", 0)).longValue(),
                (BigDecimal) data.getOrDefault("avg_sync_latency_seconds", BigDecimal.ZERO)
        );
    }

    /**
     * 2. SECURITY ALERTS (Pulling directly from vw_suspicious_patterns view)
     */
    @Override
    public List<Map<String, Object>> getSuspiciousPatterns() {
        return analyticsRepository.getSuspiciousPatterns();
    }

    /**
     * 3. VERIFICATION TRENDS (Pulling directly from vw_verification_trends view)
     */
    @Override
    public List<Map<String, Object>> getVerificationTrends() {
        return analyticsRepository.getVerificationTrends();
    }

    /**
     * 4. EXPIRY RISK
     * Accounting for secondary packaging (batches) and their primary packaging (units)
     */
    @Override
    public List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);

        return registryRepo.findAll().stream()
                .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(thresholdDate))
                .filter(batch -> !"DISPENSED".equals(batch.getCurrentStatus()) && !"DESTROYED".equals(batch.getCurrentStatus()))
                .map(batch -> new ExpiryRiskDto(
                        batch.getProductName(),
                        batch.getBatchNumber(),
                        batch.getExpiryDate(),
                        // Include context about the primary packaging units contained within this batch
                        "Contains " + batch.getBatchUnitCount() + " primary units at risk.",
                        ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 5. QUALITY REPORTS
     */
    @Override
    public List<LabQualityReportDto> getLabQualityStats() {
        List<Object[]> stats = scrutinyRepo.getFailureRatesByManufacturer();
        List<LabQualityReportDto> report = new ArrayList<>();

        for (Object[] row : stats) {
            String manufacturer = (String) row[0];
            long total = ((Number) row[1]).longValue();
            long failures = ((Number) row[2]).longValue();
            long passed = total - failures;

            double rate = total == 0 ? 0 : ((double) failures / total) * 100;

            report.add(new LabQualityReportDto(
                    manufacturer,
                    total,
                    passed,
                    failures,
                    rate
            ));
        }
        return report;
    }
}