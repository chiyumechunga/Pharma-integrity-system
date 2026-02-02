package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.dto.analytics.SuspiciousScanDto;
import com.chiyumechunga.backend.repository.ChainOfCustodyRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.ProductVerificationRepository;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProductVerificationRepository verificationRepo;
    private final PharmaceuticalRegistryRepository registryRepo;
    private final RegulatoryScrutinyRepository scrutinyRepo;
    private final SupplyChainParticipantRepository participantRepo;
    private final ChainOfCustodyRepository custodyRepo; // Added for transfer stats

    /**
     * D. EXECUTIVE OVERVIEW
     * Logic: Aggregates all 8 KPIs required by the DashboardStatsDto.
     */
    @Override
    public DashboardStatsDto getDashboardOverview() {
        // 1. Batch Stats
        long authenticBatches = registryRepo.countByCurrentStatus("CONFIRMED"); // or "ON_CHAIN" depending on your flow
        long pendingBatches = registryRepo.countByCurrentStatus("PENDING_BLOCKCHAIN");
        long failedBatches = registryRepo.countByCurrentStatus("BLOCKCHAIN_FAILED");

        // 2. Scan Stats
        long totalScans = verificationRepo.count();
        long authenticScans = verificationRepo.countByVerificationStatus("AUTHENTIC");
        long flaggedScans = totalScans - authenticScans; // Expired, Counterfeit, etc.

        // 3. Authenticity Rate Calculation
        BigDecimal authenticityRate = BigDecimal.ZERO;
        if (totalScans > 0) {
            authenticityRate = BigDecimal.valueOf(authenticScans)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalScans), 2, RoundingMode.HALF_UP);
        }

        // 4. Activity Stats
        long totalTransfers = custodyRepo.count();

        // FIX: Initialize as BigDecimal to match new DTO signature
        BigDecimal avgLatency = BigDecimal.ZERO;

        // 5. Return Full DTO (8 Arguments)
        return new DashboardStatsDto(
                authenticBatches,
                pendingBatches,
                failedBatches,
                authenticScans,
                flaggedScans,
                authenticityRate,
                totalTransfers,
                avgLatency
        );
    }

    /**
     * SECURITY ALERTS
     */
    @Override
    public List<SuspiciousScanDto> getSuspiciousScanAlerts() {
        List<Object[]> anomalies = verificationRepo.findPotentialClones();
        List<SuspiciousScanDto> alerts = new ArrayList<>();

        for (Object[] row : anomalies) {
            String qrHash = row[0].toString();
            long count = ((Number) row[1]).longValue();

            alerts.add(new SuspiciousScanDto(
                    qrHash,
                    count,
                    "High Scan Volume (Potential Clone)",
                    LocalDate.now().toString()
            ));
        }
        return alerts;
    }

    /**
     * EXPIRY RISK
     */
    @Override
    public List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);

        return registryRepo.findAll().stream()
                .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(thresholdDate))
                .filter(batch -> !"DISPENSED".equals(batch.getCurrentStatus()))
                .map(batch -> new ExpiryRiskDto(
                        batch.getProductName(),
                        batch.getBatchNumber(),
                        batch.getExpiryDate(),
                        "Unknown",
                        ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate())
                ))
                .collect(Collectors.toList());
    }

    /**
     * QUALITY REPORTS
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