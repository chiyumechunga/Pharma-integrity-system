package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.AuditService; // Import the new service
import com.chiyumechunga.backend.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final AuditService auditService; // Use AuditService instead of Repository directly

    public VerificationServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                   AuditService auditService) {
        this.registryRepository = registryRepository;
        this.auditService = auditService;
    }

    @Override
    public VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo) {
        // 1. QUERY
        PharmaceuticalRegistry product = registryRepository.findByQrHash(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 2. CHECK STATUS
        boolean isValid = "ON_CHAIN".equals(product.getCurrentStatus());

        // 3. LOG SCAN (Delegated to AuditService to ensure @Async works)
        auditService.logScanAsync(product, deviceFingerprint, geo, isValid ? "AUTHENTIC" : "SUSPICIOUS");

        // 4. RETURN
        return new VerificationResponseDto(
                product.getProductName(),
                product.getCurrentStatus(),
                product.getBlockchainTxId(),
                isValid,
                isValid ? "Verified Authentic" : "Invalid Status",
                null // The fix for Problem 2
        );
    }
}