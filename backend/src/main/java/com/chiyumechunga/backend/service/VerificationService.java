package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.VerificationResponseDto;

public interface VerificationService {
    // You must define the "contract" here so the Implementation can override it
    VerificationResponseDto verifyProduct(String qrHash, String deviceFingerprint, String geo);
}
