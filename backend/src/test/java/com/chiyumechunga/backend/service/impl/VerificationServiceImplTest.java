package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.AuditService;
import com.chiyumechunga.backend.service.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private PharmaceuticalRegistryRepository registryRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Test
    void shouldVerifyAuthenticProductSuccessfully() {
        // 1. SETUP
        String qrHash = "valid-qr-hash-123";
        String deviceFp = "mobile-app-user-456";
        String geo = "-15.3875, 28.3228"; // Lusaka coordinates

        PharmaceuticalRegistry mockProduct = new PharmaceuticalRegistry();
        mockProduct.setProductName("Coartem");
        mockProduct.setCurrentStatus("ON_CHAIN"); // Authentic status
        mockProduct.setBlockchainTxId("tx-001");

        Mockito.when(registryRepository.findByQrHash(qrHash))
                .thenReturn(Optional.of(mockProduct));

        // 2. EXECUTE
        VerificationResponseDto result = verificationService.verifyProduct(qrHash, deviceFp, geo);

        // 3. VERIFY
        Assertions.assertTrue(result.isValid());
        Assertions.assertEquals("Verified Authentic", result.message());

        // Verify Audit Log was called with "AUTHENTIC"
        Mockito.verify(auditService, Mockito.times(1))
                .logScanAsync(mockProduct, deviceFp, geo, "AUTHENTIC");

        // Verify NO ALERT was sent
        Mockito.verify(notificationService, Mockito.never()).sendAdminAlert(any(), any());
    }

    @Test
    void shouldDetectSuspiciousProductAndSendAlert() {
        // 1. SETUP
        String qrHash = "stolen-qr-hash-999";

        PharmaceuticalRegistry mockProduct = new PharmaceuticalRegistry();
        mockProduct.setProductName("Panadol");
        mockProduct.setCurrentStatus("RECALLED"); // NOT "ON_CHAIN", so it's invalid

        Mockito.when(registryRepository.findByQrHash(qrHash))
                .thenReturn(Optional.of(mockProduct));

        // 2. EXECUTE
        VerificationResponseDto result = verificationService.verifyProduct(qrHash, "device-789", "geo-000");

        // 3. VERIFY
        Assertions.assertFalse(result.isValid());
        Assertions.assertTrue(result.message().contains("Invalid Status"));

        // Critical Security Verification: Did the alert system trigger?
        Mockito.verify(notificationService, Mockito.times(1))
                .sendAdminAlert(eq("Counterfeit/Invalid Drug Detected! QR: " + qrHash), eq("HIGH"));

        // Verify Audit Log was called with "SUSPICIOUS"
        Mockito.verify(auditService, Mockito.times(1))
                .logScanAsync(eq(mockProduct), any(), any(), eq("SUSPICIOUS"));
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        String invalidQrHash = "non-existent-qr";

        Mockito.when(registryRepository.findByQrHash(invalidQrHash))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            verificationService.verifyProduct(invalidQrHash, "dev", "geo");
        });
    }
}