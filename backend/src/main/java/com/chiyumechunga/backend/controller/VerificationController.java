package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.IncidentReportDto;
import com.chiyumechunga.backend.dto.VerificationRequestDto;
import com.chiyumechunga.backend.dto.VerificationResponseDto;
import com.chiyumechunga.backend.service.QrCodeService;
import com.chiyumechunga.backend.service.VerificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final VerificationService verificationService;
    private final QrCodeService qrCodeService;

    public VerificationController(VerificationService verificationService, QrCodeService qrCodeService) {
        this.verificationService = verificationService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping
    public ResponseEntity<VerificationResponseDto> verifyProduct(@Valid @RequestBody VerificationRequestDto request) {
        log.info("Verification scan request received for QR: {}", request.qrHash());

        // Sanitize client inputs
        String safeDevice = HtmlUtils.htmlEscape(request.deviceFingerprint());
        String safeGeo = HtmlUtils.htmlEscape(request.geoLocation());

        // Extract Role from Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String scannedByRole = "PUBLIC";

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            scannedByRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("PUBLIC");
        }

        // Execute Verification
        VerificationResponseDto response = verificationService.verifyProduct(
                request.qrHash(),
                safeDevice,
                safeGeo,
                scannedByRole
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping(value = "/generate-label/{qrHash}")
    public ResponseEntity<byte[]> generateBatchLabel(@PathVariable String qrHash) {
        // Calling the generic payload method for arbitrary hashes
        byte[] image = qrCodeService.generateQrCodeImage(qrHash, 250, 250);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    @PostMapping("/report")
    public ResponseEntity<?> submitReport(@RequestBody IncidentReportDto reportDto) {
        verificationService.submitPublicReport(reportDto);
        return ResponseEntity.ok().build();
    }
}