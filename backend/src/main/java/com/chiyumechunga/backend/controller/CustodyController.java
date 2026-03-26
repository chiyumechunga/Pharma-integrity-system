package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.service.CustodyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/custody")
public class CustodyController {

    private final CustodyService custodyService;

    public CustodyController(CustodyService custodyService) {
        this.custodyService = custodyService;
    }

    // 1. CREATE CUSTODY TRANSFER (Removed /transfer verb)
    @PostMapping
    public ResponseEntity<FireflyAckDto> transferProduct(@Valid @RequestBody CustodyTransferRequestDto request) {
        log.info("Received Custody Transfer Request | Batch: {} | From: {} | To: {}",
                request.batchNumber(), request.fromParticipantId(), request.toParticipantId());

        FireflyAckDto response = custodyService.transferCustody(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    // 2. GET SPECIFIC CUSTODY EVENT (Not Implemented - Missing Service Method)
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustodyEvent(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement custodyService.getCustodyEventById(id).");
    }
}