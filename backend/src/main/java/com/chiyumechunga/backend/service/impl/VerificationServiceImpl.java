package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.AuditService;
import com.chiyumechunga.backend.service.NotificationService;
import com.chiyumechunga.backend.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public VerificationServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                   AuditService auditService,
                                   NotificationService notificationService) {
        this.registryRepository = registryRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Override
    public VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo) {
        PharmaceuticalRegistry product = registryRepository.findByQrHash(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with QR: " + qrHash));

        // 1. FIX: Check against DDL-compliant states
        String status = product.getCurrentStatus();
        boolean isAuthenticState = "CONFIRMED".equals(status) || "DISPENSED".equals(status);

        // 2. FIX: Ensure the drug isn't expired
        boolean isNotExpired = product.getExpiryDate() != null && product.getExpiryDate().isAfter(LocalDate.now());

        boolean isValid = isAuthenticState && isNotExpired;

        // 3. SECURITY ALERT (Granular reporting)
        if (!isValid) {
            String alertReason = !isNotExpired ? "Expired Drug" : "Invalid Status (" + status + ")";
            log.warn("Suspicious scan detected for QR: {}. Reason: {}", qrHash, alertReason);
            notificationService.sendAdminAlert(
                    alertReason + " Detected! QR: " + qrHash,
                    "HIGH"
            );
        }

        auditService.logScanAsync(product, deviceFingerprint, geo, isValid ? "AUTHENTIC" : "SUSPICIOUS");

        String responseMessage = isValid ? "Verified Authentic" :
                (!isNotExpired ? "Warning: Product is Expired." : "Invalid Status: Product may be counterfeit or recalled.");

        return new VerificationResponseDto(
                product.getProductName(),
                status,
                product.getBlockchainTxId(),
                isValid,
                responseMessage,
                null
        );
    }
}