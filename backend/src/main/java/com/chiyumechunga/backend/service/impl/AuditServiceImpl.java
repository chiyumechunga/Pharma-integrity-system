package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductVerification;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.repository.ProductVerificationRepository;
import com.chiyumechunga.backend.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    private final ProductVerificationRepository verificationRepository;

    public AuditServiceImpl(ProductVerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    // Notice we only have ONE logScanAsync method now (the 6-parameter one)
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logScanAsync(PharmaceuticalRegistry batch, SerializedUnit unit, String deviceFingerprint, String geo, String status, String role) {
        try {
            ProductVerification verification = new ProductVerification();

            // Safely map the Batch (might be null if it's a counterfeit scan!)
            if (batch != null) {
                verification.setPharmaceuticalRegistry(batch);
            }

            // Map the specific unit if it exists
            if (unit != null) {
                verification.setSerializedUnit(unit);
            }

            verification.setDeviceFingerprint(deviceFingerprint);
            verification.setGeoLocation(geo);
            verification.setVerificationStatus(status);
            verification.setScannedByRole(role);
            verification.setScanTimestamp(LocalDateTime.now());

            verificationRepository.save(verification);

            String identifier = unit != null ? unit.getQrHash() : (batch != null ? batch.getQrHash() : "COUNTERFEIT_QR");
            log.info("Audit log saved asynchronously for QR: {} by role: {}", identifier, role);

        } catch (Exception e) {
            log.error("Failed to save audit log for role: {}", role, e);
        }
    }

    @Override
    public List<ProductVerification> getAllAuditLogs() {
        return verificationRepository.findAll();
    }
}