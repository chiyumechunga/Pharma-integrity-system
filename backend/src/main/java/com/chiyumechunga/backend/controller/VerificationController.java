package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.VerificationRequestDto;
import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.service.VerificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    // 1. LOG VERIFICATION SCAN (Removed /scan verb)
    @PostMapping
    public ResponseEntity<VerificationResponseDto> verifyProduct(@Valid @RequestBody VerificationRequestDto request) {
        log.info("Verification scan request received for QR: {} from Location: {}",
                request.qrHash(), request.geoLocation());

        String safeQr = sanitize(request.qrHash());
        String safeDevice = sanitize(request.deviceFingerprint());
        String safeGeo = sanitize(request.geoLocation());

        VerificationResponseDto result = verificationService.verifyProduct(safeQr, safeDevice, safeGeo);
        return ResponseEntity.ok(result);
    }

    // 2. LIST ALL VERIFICATIONS (Not Implemented - Missing Service Method)
    @GetMapping
    public ResponseEntity<?> listVerifications() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement verificationService.getAllVerifications().");
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input);
    }
}