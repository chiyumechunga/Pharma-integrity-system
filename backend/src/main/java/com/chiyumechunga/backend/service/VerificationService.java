package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.IncidentReportDto;
import com.chiyumechunga.backend.dto.VerificationResponseDto;

public interface VerificationService {

    /**
     * Resolves a QR hash, validates the product state/expiry, triggers necessary alerts,
     * and delegates audit logging.
     */
    VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo, String scannedByRole);

    /**
     * Accepts public reports for suspicious, recalled, or missing products.
     */
    void submitPublicReport(IncidentReportDto reportDto);
}