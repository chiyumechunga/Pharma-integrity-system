package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.provenance.ProductDetailsDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceEventDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceResponseDto;
import com.chiyumechunga.backend.service.ProvenanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/provenance")
public class ProvenanceController {

    private final ProvenanceService provenanceService;

    public ProvenanceController(ProvenanceService provenanceService) {
        this.provenanceService = provenanceService;
    }

    @GetMapping("/{qrHash}")
    public ResponseEntity<ProvenanceResponseDto> getProductHistory(@PathVariable String qrHash) {
        String safeQrHash = sanitizeStrict(qrHash);
        log.info("Provenance request for QR: {}", safeQrHash);

        ProvenanceResponseDto rawHistory = provenanceService.getProvenance(safeQrHash);
        ProvenanceResponseDto safeHistory = sanitizeResponse(rawHistory);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return ResponseEntity.ok()
                .headers(headers)
                .body(safeHistory);
    }

    /**
     * Helper: Recursively sanitizes the nested DTO structure to prevent XSS.
     */
    private ProvenanceResponseDto sanitizeResponse(ProvenanceResponseDto input) {
        if (input == null) return null;

        // 1. Sanitize Product Details (Null-safe)
        ProductDetailsDto safeDetails = new ProductDetailsDto(
                safeEscape(input.productDetails().genericName()),
                safeEscape(input.productDetails().batchNumber()),
                safeEscape(input.productDetails().manufacturer()),
                input.productDetails().serialNumber(), // Preserved as raw if not a String
                input.productDetails().expiryDate(),
                safeEscape(input.productDetails().currentStatus())
        );

        // 2. Sanitize the Timeline Events (Null-safe)
        List<ProvenanceEventDto> safeEvents = input.provenanceTimeline().stream()
                .map(e -> new ProvenanceEventDto(
                        e.eventTimestamp(),
                        safeEscape(e.eventType()),
                        safeEscape(e.fromParticipant()),
                        safeEscape(e.fromParticipantName()),
                        safeEscape(e.toParticipant()),
                        safeEscape(e.toParticipantName()),
                        safeEscape(e.blockchainTxId())
                )).toList();

        // 3. Assemble the Safe Root DTO
        return new ProvenanceResponseDto(
                safeEscape(input.verificationStatus()),
                input.scanTimestamp(),
                safeDetails,
                safeEvents
        );
    }

    private String sanitizeStrict(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }

    /**
     * Null-safe HTML escape helper.
     */
    private String safeEscape(String input) {
        return input != null ? HtmlUtils.htmlEscape(input) : "N/A";
    }
}