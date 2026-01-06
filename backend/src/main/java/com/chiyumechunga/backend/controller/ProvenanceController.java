package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.provenance.FullProvenanceDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceEventDto;
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

    /**
     * Public Endpoint: View the full lifecycle of a product.
     * Includes OUTPUT SANITIZATION to prevent Stored XSS.
     */
    @GetMapping("/{qrHash}")
    public ResponseEntity<FullProvenanceDto> getProductHistory(@PathVariable String qrHash) {
        // 1. INPUT SANITIZATION (Prevent Injection Attacks)
        String safeQrHash = sanitizeStrict(qrHash);

        log.info("Provenance request for QR: {}", safeQrHash);

        // 2. EXECUTE READ (Get potentially tainted data from DB)
        FullProvenanceDto rawHistory = provenanceService.getProvenance(safeQrHash);

        // 3. OUTPUT SANITIZATION (The Fix for the Security Tool)
        // We explicitly escape every string coming from the DB before sending it to the client.
        // This breaks the "Stored XSS" taint chain.
        FullProvenanceDto safeHistory = sanitizeResponse(rawHistory);

        // 4. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return ResponseEntity.ok()
                .headers(headers)
                .body(safeHistory);
    }

    /**
     * Helper: Recursively sanitizes the DTO to render scripts harmless.
     * e.g. "<script>" becomes "&lt;script&gt;"
     */
    private FullProvenanceDto sanitizeResponse(FullProvenanceDto input) {
        if (input == null) return null;

        // 1. Sanitize the List of Events
        List<ProvenanceEventDto> safeEvents = input.history().stream()
                .map(e -> new ProvenanceEventDto(
                        HtmlUtils.htmlEscape(e.eventType()),
                        HtmlUtils.htmlEscape(e.fromParticipant()), // Critical: User names are high-risk
                        HtmlUtils.htmlEscape(e.toParticipant()),
                        e.timestamp(),
                        HtmlUtils.htmlEscape(e.blockchainTxId())
                )).toList();

        // 2. Sanitize the Parent DTO
        return new FullProvenanceDto(
                HtmlUtils.htmlEscape(input.productName()),
                HtmlUtils.htmlEscape(input.batchNumber()),
                HtmlUtils.htmlEscape(input.manufacturerName()),
                input.expiryDate(), // Safe: LocalDate cannot hold scripts
                HtmlUtils.htmlEscape(input.currentStatus()),
                safeEvents
        );
    }

    /**
     * Strict Sanitizer for URL parameters.
     */
    private String sanitizeStrict(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}