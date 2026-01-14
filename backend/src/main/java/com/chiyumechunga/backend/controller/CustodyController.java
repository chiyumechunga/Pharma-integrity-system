package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto; // <--- FIX 1: Correct Import
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.service.CustodyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/custody")
public class CustodyController {

    private final CustodyService custodyService;

    public CustodyController(CustodyService custodyService) {
        this.custodyService = custodyService;
    }

    /**
     * Endpoint for Distributors/Pharmacies to accept custody of a product.
     * @param request Contains Batch Number, Sender ID, Receiver ID.
     * @return Acknowledgment from Blockchain (Operation ID).
     */
    @PostMapping("/transfer")
    // FIX 2: Update parameter type to 'CustodyTransferRequestDto'
    public ResponseEntity<FireflyAckDto> transferProduct(@Valid @RequestBody CustodyTransferRequestDto request) {

        // FIX 3: Use 'batchNumber()' instead of 'qrHash()' (matching the DTO)
        log.info("Received Custody Transfer Request | Batch: {} | From: {} | To: {}",
                request.batchNumber(), request.fromParticipantId(), request.toParticipantId());

        // 1. CALL SERVICE
        FireflyAckDto response = custodyService.transferCustody(request);

        // 2. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. RETURN RESPONSE (202 Accepted)
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }
}