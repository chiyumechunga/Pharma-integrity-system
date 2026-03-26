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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/regulatory")
public class RegulatoryController {

    private final RegulatoryService regulatoryService;

    public RegulatoryController(RegulatoryService regulatoryService) {
        this.regulatoryService = regulatoryService;
    }

    // 1. CREATE SCRUTINY EVENT (Renamed from /inspections)
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

    // 2. GET SCRUTINY EVENT DETAILS (Not Implemented - Missing Service Method)
    @GetMapping("/scrutiny/{id}")
    public ResponseEntity<?> getInspection(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement regulatoryService.getInspectionById(id).");
    }

    // 3. LIST ALL RECALLS (Not Implemented - Missing Service Method)
    @GetMapping("/recalls")
    public ResponseEntity<?> listRecalls() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement regulatoryService.getAllRecalls().");
    }
}