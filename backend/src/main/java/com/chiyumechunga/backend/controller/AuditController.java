package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.model.ProductVerification;
import com.chiyumechunga.backend.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/scans")
    public ResponseEntity<List<ProductVerification>> getAllScanLogs() {
        log.info("Admin dashboard fetching all verification audit logs");
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }
}