package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.AuditService;
import com.chiyumechunga.backend.service.NotificationService; // Import NotificationService
import com.chiyumechunga.backend.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService; // <--- ADD THIS FIELD

    // Inject all three services
    public VerificationServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                   AuditService auditService,
                                   NotificationService notificationService) { // <--- ADD TO CONSTRUCTOR
        this.registryRepository = registryRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    // VerificationServiceImpl.java
    @Override
    public VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo) {
        PharmaceuticalRegistry product = registryRepository.findByQrHash(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with QR: " + qrHash));

        // FIX: Use DDL-compliant status and check expiry
        boolean isConfirmed = "CONFIRMED".equals(product.getCurrentStatus());
        boolean isNotExpired = product.getExpiryDate().isAfter(java.time.LocalDate.now());
        boolean isValid = isConfirmed && isNotExpired;

        if (!isValid) {
            String alertReason = !isNotExpired ? "Expired Drug" : "Counterfeit/Invalid Status";
            log.warn("Suspicious scan detected for QR: {}. Reason: {}", qrHash, alertReason);
            notificationService.sendAdminAlert(alertReason + " Detected! QR: " + qrHash, "HIGH");
        }

        auditService.logScanAsync(product, deviceFingerprint, geo, isValid ? "AUTHENTIC" : "SUSPICIOUS");

        String responseMessage = isValid ? "Verified Authentic" :
                (!isNotExpired ? "Warning: Product is Expired." : "Invalid Status: Product may be counterfeit.");

        return new VerificationResponseDto(
                product.getProductName(),
                product.getCurrentStatus(),
                product.getBlockchainTxId(),
                isValid,
                responseMessage,
                null
        );
    }
}