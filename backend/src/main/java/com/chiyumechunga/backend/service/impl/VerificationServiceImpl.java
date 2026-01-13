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

    @Override
    public VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo) {
        // 1. QUERY
        PharmaceuticalRegistry product = registryRepository.findByQrHash(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with QR: " + qrHash));

        // 2. CHECK STATUS
        // We assume the product is valid only if its status matches the expected "ON_CHAIN" state.
        boolean isValid = "ON_CHAIN".equals(product.getCurrentStatus());

        // 3. SECURITY ALERT (If fake/invalid)
        if (!isValid) { // <--- FIX: Use the 'isValid' variable calculated above
            log.warn("Suspicious scan detected for QR: {}", qrHash);
            notificationService.sendAdminAlert(
                    "Counterfeit/Invalid Drug Detected! QR: " + qrHash, // <--- FIX: Use 'qrHash' parameter directly
                    "HIGH"
            );
        }

        // 4. LOG SCAN (Delegated to AuditService to ensure @Async works)
        auditService.logScanAsync(product, deviceFingerprint, geo, isValid ? "AUTHENTIC" : "SUSPICIOUS");

        // 5. RETURN
        return new VerificationResponseDto(
                product.getProductName(),
                product.getCurrentStatus(),
                product.getBlockchainTxId(),
                isValid,
                isValid ? "Verified Authentic" : "Invalid Status: Product may be counterfeit or expired.",
                null // extraNotes can be null
        );
    }
}