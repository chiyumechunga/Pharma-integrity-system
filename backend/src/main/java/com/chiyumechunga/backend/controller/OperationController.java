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
        // Assumption Flag: fireflyIntegrationService needs a getOperationStatus() method
        // that queries the Firefly API: GET /api/v1/namespaces/default/operations/{id}

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement fireflyIntegrationService.getOperationStatus(operationId).");
    }
}