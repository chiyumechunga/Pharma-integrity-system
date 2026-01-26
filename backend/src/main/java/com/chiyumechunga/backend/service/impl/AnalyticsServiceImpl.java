package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import com.chiyumechunga.backend.dto.analytics.ExpiryRiskDto;
import com.chiyumechunga.backend.dto.analytics.LabQualityReportDto;
import com.chiyumechunga.backend.dto.analytics.SuspiciousScanDto;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.ProductVerificationRepository;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * D. EXECUTIVE OVERVIEW
     * Logic: Aggregates total counts. Updated to use String status instead of boolean.
     */
    @Override
    public DashboardStatsDto getDashboardOverview() {
        long totalBatches = registryRepo.count();
        long totalParticipants = participantRepo.count();

        // FIX: The database now uses strings ('AUTHENTIC', 'COUNTERFEIT', etc.)
        // instead of a simple boolean 'isValid'.
        long successScans = verificationRepo.countByVerificationStatus("AUTHENTIC");

        // We calculate failed scans by subtracting authentic ones from the total
        long totalScans = verificationRepo.count();
        long failedScans = totalScans - successScans;

        return new DashboardStatsDto(
                totalBatches,
                totalParticipants,
                failedScans,
                successScans
        );
    }

    /**
     * SECURITY ALERTS
     * Logic: uses the custom JPQL query 'findPotentialClones' to find QR hashes
     * that appear in multiple locations or have excessive scan counts.
     */
    @Override
    public List<SuspiciousScanDto> getSuspiciousScanAlerts() {
        // This relies on the custom query we added to ProductVerificationRepository
        List<Object[]> anomalies = verificationRepo.findPotentialClones();
        List<SuspiciousScanDto> alerts = new ArrayList<>();

        for (Object[] row : anomalies) {
            // Row[0] = QR Hash, Row[1] = Count
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
     * Logic: Filters the registry for items expiring soon that haven't been dispensed.
     */
    @Override
    public List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);

        return registryRepo.findAll().stream()
                .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(thresholdDate))
                .filter(batch -> !"DISPENSED".equals(batch.getCurrentStatus())) // Ignore items already sold
                .map(batch -> new ExpiryRiskDto(
                        batch.getProductName(),
                        batch.getBatchNumber(),
                        batch.getExpiryDate(),
                        "Unknown", // Owner logic can be complex, skipping for basic overview
                        ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate())
                ))
                .collect(Collectors.toList());
    }

    /**
     * QUALITY REPORTS
     * Logic: Uses the custom JPQL query 'getFailureRatesByManufacturer' to aggregate lab results.
     */
    @Override
    public List<LabQualityReportDto> getLabQualityStats() {
        List<Object[]> stats = scrutinyRepo.getFailureRatesByManufacturer();
        List<LabQualityReportDto> report = new ArrayList<>();

        for (Object[] row : stats) {
            // Row structure from Query: [ManufacturerName, TotalTests, FailedTests]
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