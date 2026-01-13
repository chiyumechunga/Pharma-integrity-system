package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.TransferRequestDto;
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

    // Constructor Injection (Best Practice)
    public CustodyController(CustodyService custodyService) {
        this.custodyService = custodyService;
    }

    /**
     * Endpoint for Distributors/Pharmacies to accept custody of a product.
     * * @param request Contains QR Hash, Sender ID, Receiver ID, and Event Type.
     * @return Acknowledgment from Blockchain (Operation ID).
     */
    @PostMapping("/transfer")
    public ResponseEntity<FireflyAckDto> transferProduct(@Valid @RequestBody TransferRequestDto request) {
        log.info("Received Custody Transfer Request | QR: {} | From: {} | To: {}",
                request.qrHash(), request.fromParticipantId(), request.toParticipantId());

        // 1. CALL SERVICE
        // This triggers the blockchain transaction via Firefly
        FireflyAckDto response = custodyService.transferCustody(request);

        // 2. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. RETURN RESPONSE
        // We use HttpStatus.ACCEPTED (202) because blockchain transactions are asynchronous.
        // The request is "Accepted" for processing, but not yet "Confirmed" on-chain.
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }
}