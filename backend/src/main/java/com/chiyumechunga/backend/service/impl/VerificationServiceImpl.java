package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.IncidentReportDto;
import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.model.IncidentReport;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.repository.IncidentReportRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SerializedUnitRepository;
import com.chiyumechunga.backend.service.AuditService;
import com.chiyumechunga.backend.service.NotificationService;
import com.chiyumechunga.backend.service.VerificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final SerializedUnitRepository unitRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ADDED: The repository instance
    private final IncidentReportRepository incidentReportRepository;

    // ADDED: Injecting incidentReportRepository into the constructor
    public VerificationServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                   SerializedUnitRepository unitRepository,
                                   AuditService auditService,
                                   NotificationService notificationService,
                                   IncidentReportRepository incidentReportRepository) {
        this.registryRepository = registryRepository;
        this.unitRepository = unitRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.incidentReportRepository = incidentReportRepository;
    }

    @Override
    public VerificationResponseDto verifyProduct(String scannedCode, String deviceFingerprint, String geo, String scannedByRole) {

        PharmaceuticalRegistry product = null;
        SerializedUnit scannedUnit = null;
        String statusToCheck = "NOT_FOUND";
        boolean isUnitLevel = false;
        boolean isFound = false;

        // 1. SEARCH FOR UNIT OR BATCH
        Optional<SerializedUnit> unitOpt = unitRepository.findByQrHash(scannedCode);
        if (unitOpt.isPresent()) {
            scannedUnit = unitOpt.get();
            product = registryRepository.findById(scannedUnit.getRegistryId()).orElse(null);
            statusToCheck = scannedUnit.getCurrentStatus();
            isUnitLevel = true;
            isFound = (product != null);
        } else {
            // Fallback: Check if it's a bulk transit Batch
            Optional<PharmaceuticalRegistry> batchOpt = registryRepository.findByQrHash(scannedCode);
            if (batchOpt.isPresent()) {
                product = batchOpt.get();
                statusToCheck = product.getCurrentStatus();
                isFound = true;
            }
        }

        // 2. HANDLE COUNTERFEIT / UNREGISTERED
        if (!isFound) {
            log.warn("COUNTERFEIT DETECTED: Unknown QR scanned: {}", scannedCode);
            notificationService.sendAdminAlert("Counterfeit scan attempt! Unknown Hash: " + scannedCode, "CRITICAL");

            // Log the fake scan. Pass nulls for product/unit since they don't exist.
            auditService.logScanAsync(null, null, deviceFingerprint, geo, "COUNTERFEIT", scannedByRole);

            return new VerificationResponseDto(
                    "Unknown Product",
                    "NOT_ON_LEDGER",
                    null, // No blockchain TX for fakes
                    false,
                    "DANGER: This product does not exist on the National Ledger. It may be counterfeit. Please report this immediately.",
                    null
            );
        }

        // 3. STATUS EVALUATION
        boolean isExpired = product.getExpiryDate() != null && product.getExpiryDate().isBefore(LocalDate.now());
        boolean isRecalled = "RECALLED".equals(statusToCheck) || "RECALLED".equals(product.getCurrentStatus());
        boolean isAuthenticState = "CONFIRMED".equals(product.getCurrentStatus());

        // Unit-specific status rules
        if (isUnitLevel) {
            isAuthenticState = isAuthenticState && ("IN_BATCH".equals(statusToCheck) || "AVAILABLE".equals(statusToCheck) || "DISPENSED".equals(statusToCheck));
        } else {
            isAuthenticState = isAuthenticState && ("CONFIRMED".equals(statusToCheck) || "DISPENSED".equals(statusToCheck));
        }

        boolean isValid = isAuthenticState && !isExpired && !isRecalled;

        // 4. GENERATE ALERTS
        if (!isValid) {
            String alertReason = isRecalled ? "Recalled Product" : (isExpired ? "Expired Drug" : "Invalid Status");
            log.warn("Suspicious scan detected for Code: {}. Reason: {}", scannedCode, alertReason);
            notificationService.sendAdminAlert(alertReason + " Detected! QR/Serial: " + scannedCode, "HIGH");
        }

        // 5. DELEGATE TO AUDIT MODULE
        String auditStatus = isValid ? "AUTHENTIC" : (isRecalled ? "RECALLED" : "SUSPICIOUS");
        auditService.logScanAsync(product, scannedUnit, deviceFingerprint, geo, auditStatus, scannedByRole);

        // 6. BUILD RESPONSE
        String responseMessage;
        if (isRecalled) {
            responseMessage = "Danger: Product has been Recalled. Do not consume. Please report the pharmacy.";
        } else if (isExpired) {
            responseMessage = "Warning: Product is Expired. Please report this location.";
        } else if (isUnitLevel && "DISPENSED".equals(statusToCheck)) {
            responseMessage = "Warning: This specific unit was already sold previously. If you are buying this new, please report it.";
        } else if (isValid) {
            responseMessage = "Verified Authentic. Safe to consume.";
        } else {
            responseMessage = "Invalid Status: Product may be compromised.";
        }

        return new VerificationResponseDto(
                product.getProductName(),
                statusToCheck,
                product.getBlockchainTxId(),
                isValid,
                responseMessage,
                null
        );
    }

    @Override
    @Transactional
    public void submitPublicReport(IncidentReportDto reportDto) {
        log.info("New public incident report received for QR: {}", reportDto.getQrHash());

        // FIX: Build the actual IncidentReport Entity, not the DTO
        IncidentReport report = IncidentReport.builder()
                .qrHash(reportDto.getQrHash())
                .reporterName(reportDto.getReporterName())
                .reporterPhoneOrEmail(reportDto.getReporterPhoneOrEmail())
                .locationInfo(reportDto.getLocationInfo())
                .incidentDescription(reportDto.getIncidentDescription())
                .issueType(reportDto.getIssueType() != null ? reportDto.getIssueType() : "UNKNOWN")
                .status("PENDING_REVIEW")
                .reportedAt(OffsetDateTime.now())
                .build();

        // FIX: Save using the injected repository
        incidentReportRepository.save(report);

        // 2. Trigger high-priority alert to ZAMRA / Admins with the exact details
        String alertMsg = String.format("PUBLIC REPORT [%s]: %s reported at %s. Comments: %s",
                reportDto.getIssueType(),
                reportDto.getQrHash() != null ? reportDto.getQrHash() : "No QR Provided",
                reportDto.getLocationInfo(),
                reportDto.getIncidentDescription());

        notificationService.sendAdminAlert(alertMsg, "CRITICAL");

        log.info("Incident report successfully saved and admins notified.");
    }
}