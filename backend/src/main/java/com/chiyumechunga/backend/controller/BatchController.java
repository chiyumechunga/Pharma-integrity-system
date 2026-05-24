package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceResponseDto;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
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
import com.chiyumechunga.backend.service.QrCodeService;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {

    private final RegistryService registryService;
    private final ProvenanceService provenanceService;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final QrCodeService qrCodeService;

    public BatchController(RegistryService registryService, ProvenanceService provenanceService, PharmaceuticalRegistryRepository registryRepository, QrCodeService qrCodeService) {
        this.registryService = registryService;
        this.provenanceService = provenanceService;
        this.registryRepository = registryRepository;
        this.qrCodeService = qrCodeService;
    }

    /**
     * 1. CREATE BATCH
     *
     * Important correction:
     * RegistryRequestDto no longer carries productName.
     * The product is now identified by productId, and RegistryServiceImpl resolves the
     * authoritative name from product_master.generic_name.
     */
    @PostMapping
    public ResponseEntity<FireflyAckDto> registerBatch(@Valid @RequestBody RegistryRequestDto request) {

        // Rebuild a sanitized request object.
        // productId and dates are typed values, so only batchNumber needs strict sanitization.
        RegistryRequestDto safeRequest = new RegistryRequestDto(
                request.productId(),
                sanitizeStrict(request.batchNumber()),
                request.manufacturerId(),
                request.manufacturingDate(),
                request.expiryDate(),
                request.batchUnitCount()
        );

        FireflyAckDto serviceResponse = registryService.registerBatch(safeRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(serviceResponse, headers, HttpStatus.ACCEPTED);
    }

    /**
     * 2. GET BATCH DETAILS
     */
    @GetMapping("/{batchNumber}")
    public ResponseEntity<PharmaceuticalRegistry> getBatch(@PathVariable String batchNumber) {
        return ResponseEntity.ok(registryService.getBatchDetails(sanitizeStrict(batchNumber)));
    }

    /**
     * 3. GET BATCH HISTORY (PROVENANCE)
     *
     * Flow:
     * - get the batch first
     * - extract qr_hash from pharmaceutical_registry
     * - use qr_hash to fetch the provenance trail
     */
    @GetMapping("/{batchNumber}/history")
    public ResponseEntity<ProvenanceResponseDto> getBatchHistory(@PathVariable String batchNumber) {
        PharmaceuticalRegistry batch = registryService.getBatchDetails(sanitizeStrict(batchNumber));
        ProvenanceResponseDto history = provenanceService.getProvenance(batch.getQrHash());
        return ResponseEntity.ok(history);
    }

    /**
     * 4. GET QR CODE IMAGE
     *
     * Only confirmed batches should expose downloadable/printable QR codes.
     */

    /**
     * 5. LIST ALL BATCHES
     *
     * Still not implemented because RegistryService currently does not expose getAllBatches().
     */
    @GetMapping
    public ResponseEntity<List<PharmaceuticalRegistry>> listBatches() {
        // Fetches all batches from the service layer
        return ResponseEntity.ok(registryService.getAllBatches());
    }



    @GetMapping("/hash/{qrHash}")
    public ResponseEntity<?> getBatchByHash(@PathVariable String qrHash) {
        Optional<PharmaceuticalRegistry> registryOpt = registryRepository.findByQrHash(qrHash);

        if (registryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(registryOpt.get());
    }

    @GetMapping(value = "/{batchNumber}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBatchQRCode(@PathVariable String batchNumber) {
        String safeBatchNumber = sanitizeStrict(batchNumber);

        // No try-catch needed! ExceptionHandler handles 404s and 500s.
        byte[] qrCode = qrCodeService.generateBatchQrCode(safeBatchNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
    }

    /**
     * Strict sanitizer for path/input values that should remain alphanumeric with dashes/underscores only.
     */
    private String sanitizeStrict(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}