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
        // The 'labNotes' is a free-text field, making it a high risk for XSS.
        // We use HtmlUtils to escape characters (e.g., <script> becomes &lt;script&gt;)
        LabInspectionRequestDto safeRequest = new LabInspectionRequestDto(
                request.registryId(),  // UUID: Safe by type
                request.inspectorId(), // UUID: Safe by type
                request.testResult(),  // Enum: Safe by strict typing
                HtmlUtils.htmlEscape(request.labNotes()) // TEXT: Needs Escaping
        );

        // 2. PROCESS
        FireflyAckDto serviceResponse = regulatoryService.submitInspection(safeRequest);

        // 3. BREAK TAINT CHAIN
        // We construct a clean response. We do NOT return the 'labNotes' or other inputs.
        // This ensures the response is purely system-generated.
        FireflyAckDto cleanResponse = new FireflyAckDto(
                serviceResponse.operationId(), // System Generated
                "PROCESSING",                  // Hardcoded
                "Inspection data queued for blockchain." // Hardcoded
        );

        // 4. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(cleanResponse, headers, HttpStatus.ACCEPTED);
    }
}