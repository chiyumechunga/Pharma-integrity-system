package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductVerification;
import com.chiyumechunga.backend.repository.ProductVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuditService {

    private final ProductVerificationRepository verificationRepository;

    public AuditService(ProductVerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    // @Async here works perfectly because it's called from a different class (VerificationServiceImpl)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logScanAsync(PharmaceuticalRegistry product, String deviceId, String geo, String status) {
        try {
            ProductVerification scan = new ProductVerification();
            scan.setPharmaceuticalRegistry(product);
            scan.setDeviceFingerprint(deviceId);
            scan.setGeoLocation(geo);
            scan.setVerificationStatus(status);
            scan.setScannedByRole("USER");
            verificationRepository.save(scan);
            log.info("Audit log saved asynchronously for product: {}", product.getProductName());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}