package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.service.FireflyIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

    private final FireflyIntegrationService fireflyIntegrationService;

    public OperationController(FireflyIntegrationService fireflyIntegrationService) {
        this.fireflyIntegrationService = fireflyIntegrationService;
    }

    // 1. CHECK ASYNC OPERATION STATUS
    @GetMapping("/{operationId}")
    public ResponseEntity<?> getOperationStatus(@PathVariable String operationId) {
        log.info("Fetching operation status for ID: {}", operationId);
        Object status = fireflyIntegrationService.getOperationStatus(operationId);
        return ResponseEntity.ok(status);
    }
}