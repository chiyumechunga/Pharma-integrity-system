package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.dto.RecallRequestDto;
import com.chiyumechunga.backend.service.RegulatoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/regulatory")
public class RegulatoryController {

    private final RegulatoryService regulatoryService;

    public RegulatoryController(RegulatoryService regulatoryService) {
        this.regulatoryService = regulatoryService;
    }

    // 1. CREATE SCRUTINY EVENT
    @PostMapping("/scrutiny")
    public ResponseEntity<FireflyAckDto> submitInspection(@Valid @RequestBody LabInspectionRequestDto request) {
        log.info("Received Lab Inspection. Sanitizing inputs...");

        LabInspectionRequestDto safeRequest = new LabInspectionRequestDto(
                request.registryId(),
                request.inspectorId(),
                request.testResult(),
                HtmlUtils.htmlEscape(request.labNotes())
        );

        FireflyAckDto serviceResponse = regulatoryService.recordLabInspection(safeRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(serviceResponse, headers, HttpStatus.ACCEPTED);
    }

    // 2. GET SCRUTINY EVENT DETAILS
    @GetMapping("/scrutiny/{id}")
    public ResponseEntity<?> getInspection(@PathVariable UUID id) {
        return ResponseEntity.ok(regulatoryService.getInspectionById(id));
    }

    // 3. INITIATE MARKET RECALL
    @PostMapping("/recalls")
    public ResponseEntity<?> initiateRecall(@Valid @RequestBody RecallRequestDto request) {
        log.info("Received market recall request for batch: {}", request.batchNumber());

        RecallRequestDto safeRequest = new RecallRequestDto(
                HtmlUtils.htmlEscape(request.batchNumber()),
                HtmlUtils.htmlEscape(request.severityLevel()),
                HtmlUtils.htmlEscape(request.recallReason()),
                request.initiatedBy()
        );

        regulatoryService.executeRecall(safeRequest);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Recall executed successfully for " + safeRequest.batchNumber()
        ));
    }

    // 4. LIST ALL RECALLS
    @GetMapping("/recalls")
    public ResponseEntity<?> listRecalls() {
        return ResponseEntity.ok(regulatoryService.getAllRecalls());
    }
}