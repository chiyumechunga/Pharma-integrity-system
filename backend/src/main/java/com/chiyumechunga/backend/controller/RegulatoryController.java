package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.service.RegulatoryService;
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
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/regulatory")
public class RegulatoryController {

    private final RegulatoryService regulatoryService;

    public RegulatoryController(RegulatoryService regulatoryService) {
        this.regulatoryService = regulatoryService;
    }

    @PostMapping("/inspections")
    public ResponseEntity<FireflyAckDto> submitInspection(@Valid @RequestBody LabInspectionRequestDto request) {
        log.info("Received Lab Inspection. Sanitizing inputs...");

        // 1. STRICT SANITIZATION
        LabInspectionRequestDto safeRequest = new LabInspectionRequestDto(
                request.registryId(),
                request.inspectorId(),
                request.testResult(),
                HtmlUtils.htmlEscape(request.labNotes())
        );

        // 2. PROCESS - FIXED: Changed method name to match Interface
        FireflyAckDto serviceResponse = regulatoryService.recordLabInspection(safeRequest);

        // 3. BREAK TAINT CHAIN
        FireflyAckDto cleanResponse = new FireflyAckDto(
                serviceResponse.operationId(),
                "PROCESSING",
                "Inspection data queued for blockchain."
        );

        // 4. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(cleanResponse, headers, HttpStatus.ACCEPTED);
    }
}