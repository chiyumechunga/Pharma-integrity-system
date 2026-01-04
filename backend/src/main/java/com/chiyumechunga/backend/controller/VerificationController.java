package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.VerificationRequestDto;
import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.service.VerificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Endpoint for Users (Patients/Pharmacists) to scan a product.
     * This hits the Local DB for speed but returns the 'blockchainTxId' as proof.
     */
    @PostMapping("/scan")
    public ResponseEntity<VerificationResponseDto> verifyProduct(@Valid @RequestBody VerificationRequestDto request) {
        log.info("Scan request received for QR: {} from Location: {}",
                request.qrHash(), request.geoLocation());

        // 1. SANITIZATION (Security)
        // Even though this is a read-heavy op, we sanitize inputs before logging them
        // to the Audit table to prevent 'Log Injection' attacks.
        String safeQr = sanitize(request.qrHash());
        String safeDevice = sanitize(request.deviceFingerprint());
        String safeGeo = sanitize(request.geoLocation());

        // 2. CALL SERVICE
        VerificationResponseDto result = verificationService.verifyProduct(safeQr, safeDevice, safeGeo);

        // 3. RETURN RESULT
        return ResponseEntity.ok(result);
    }

    // Helper sanitizer
    private String sanitize(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input);
    }
}