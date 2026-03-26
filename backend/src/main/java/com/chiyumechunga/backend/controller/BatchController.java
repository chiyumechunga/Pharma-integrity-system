package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.dto.provenance.FullProvenanceDto;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.service.ProvenanceService;
import com.chiyumechunga.backend.service.RegistryService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {

    private final RegistryService registryService;
    private final ProvenanceService provenanceService;

    public BatchController(RegistryService registryService, ProvenanceService provenanceService) {
        this.registryService = registryService;
        this.provenanceService = provenanceService;
    }

    // 1. CREATE BATCH
    @PostMapping
    public ResponseEntity<FireflyAckDto> registerBatch(@Valid @RequestBody RegistryRequestDto request) {
        RegistryRequestDto safeRequest = new RegistryRequestDto(
                HtmlUtils.htmlEscape(request.productName()),
                sanitizeStrict(request.batchNumber()),
                request.manufacturerId(),
                request.manufacturingDate(),
                request.expiryDate()
        );

        FireflyAckDto serviceResponse = registryService.registerBatch(safeRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(serviceResponse, headers, HttpStatus.ACCEPTED);
    }

    // 2. GET BATCH DETAILS
    @GetMapping("/{batchNumber}")
    public ResponseEntity<PharmaceuticalRegistry> getBatch(@PathVariable String batchNumber) {
        return ResponseEntity.ok(registryService.getBatchDetails(sanitizeStrict(batchNumber)));
    }

    // 3. GET BATCH HISTORY (Provenance)
    @GetMapping("/{batchNumber}/history")
    public ResponseEntity<FullProvenanceDto> getBatchHistory(@PathVariable String batchNumber) {
        // Fetch the batch to get the QR Hash, then fetch the history
        PharmaceuticalRegistry batch = registryService.getBatchDetails(sanitizeStrict(batchNumber));
        FullProvenanceDto history = provenanceService.getProvenance(batch.getQrHash());
        return ResponseEntity.ok(history);
    }

    // 4. GET QR CODE IMAGE
    @GetMapping(value = "/{batchNumber}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBatchQRCode(@PathVariable String batchNumber) {
        PharmaceuticalRegistry batch = registryService.getBatchDetails(sanitizeStrict(batchNumber));

        if (!"CONFIRMED".equals(batch.getCurrentStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(batch.getQrHash(), BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return ResponseEntity.ok(pngOutputStream.toByteArray());
        } catch (Exception e) {
            log.error("ZXing QR Generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 5. LIST ALL BATCHES (Not Implemented - Missing Service Method)
    @GetMapping
    public ResponseEntity<?> listBatches() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement registryService.getAllBatches() mapping.");
    }

    // 6. INITIATE RECALL (Not Implemented - Missing Service Method)
    @PostMapping("/{batchNumber}/recall")
    public ResponseEntity<?> initiateRecall(@PathVariable String batchNumber) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("TODO: Implement regulatoryService.initiateRecall(batchNumber).");
    }

    private String sanitizeStrict(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}