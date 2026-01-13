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
import org.springframework.stereotype.Service; // <--- Critical Annotation

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    // Inject ALL required repositories here
    private final ProductVerificationRepository verificationRepo;
    private final PharmaceuticalRegistryRepository registryRepo;
    private final RegulatoryScrutinyRepository scrutinyRepo;
    private final SupplyChainParticipantRepository participantRepo;

    /**
     * D. EXECUTIVE OVERVIEW (This is where the logic belongs)
     */
    @Override
    public DashboardStatsDto getDashboardOverview() {
        long totalBatches = registryRepo.count();
        long totalParticipants = participantRepo.count();
        long failedScans = verificationRepo.countByIsValidFalse();
        long successScans = verificationRepo.countByIsValidTrue();

        return new DashboardStatsDto(
                totalBatches,
                totalParticipants,
                failedScans,
                successScans
        );
    }

    // --- OTHER METHODS (Security, Expiry, Quality) ---
    // (Ensure you include the implementations we wrote previously for getSuspiciousScanAlerts, etc.)
    @Override
    public List<SuspiciousScanDto> getSuspiciousScanAlerts() {
        List<Object[]> anomalies = verificationRepo.findPotentialClones();
        List<SuspiciousScanDto> alerts = new ArrayList<>();
        for (Object[] row : anomalies) {
            alerts.add(new SuspiciousScanDto(row[0].toString(), ((Number) row[1]).longValue(), "Multiple Locations", LocalDate.now().toString()));
        }
        return alerts;
    }

    @Override
    public List<ExpiryRiskDto> getExpiryRiskOverview(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return registryRepo.findAll().stream()
                .filter(batch -> batch.getExpiryDate().isBefore(thresholdDate))
                .filter(batch -> !"DISPENSED".equals(batch.getCurrentStatus()))
                .map(batch -> new ExpiryRiskDto(batch.getProductName(), batch.getBatchNumber(), batch.getExpiryDate(), "Unknown", ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate())))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabQualityReportDto> getLabQualityStats() {
        List<Object[]> stats = scrutinyRepo.getFailureRatesByManufacturer();
        List<LabQualityReportDto> report = new ArrayList<>();
        for (Object[] row : stats) {
            double rate = ((Number) row[1]).longValue() == 0 ? 0 : ((double) ((Number) row[2]).longValue() / ((Number) row[1]).longValue()) * 100;
            report.add(new LabQualityReportDto((String) row[0], ((Number) row[1]).longValue(), ((Number) row[1]).longValue() - ((Number) row[2]).longValue(), ((Number) row[2]).longValue(), rate));
        }
        return report;
    }
}