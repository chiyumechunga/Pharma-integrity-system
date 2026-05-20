package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductVerification;
import com.chiyumechunga.backend.model.SerializedUnit;

import java.util.List;

public interface AuditService {

    /**
     * Asynchronously records a product scan event without blocking the main user thread.
     */

    void logScanAsync(PharmaceuticalRegistry batch, SerializedUnit unit, String deviceFingerprint, String geo, String status, String role);

    /**
     * Retrieves the complete history of all product verifications for the admin dashboard.
     */
    List<ProductVerification> getAllAuditLogs();
}