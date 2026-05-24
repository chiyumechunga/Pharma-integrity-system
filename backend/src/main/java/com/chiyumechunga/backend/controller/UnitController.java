package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.DispenseRequest;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.repository.SerializedUnitRepository;
import com.chiyumechunga.backend.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final SerializedUnitRepository serializedUnitRepository;
    private final QrCodeService qrCodeService;

    public UnitController(SerializedUnitRepository serializedUnitRepository, QrCodeService qrCodeService) {
        this.serializedUnitRepository = serializedUnitRepository;
        this.qrCodeService = qrCodeService;
    }

    /**
     * 1. GET ALL UNITS
     * Fetches all individual units for the dashboard.
     */
    @GetMapping
    public ResponseEntity<List<SerializedUnit>> getAllUnits() {
        List<SerializedUnit> units = serializedUnitRepository.findAll();
        return ResponseEntity.ok(units);
    }

    @GetMapping("/{serialNumber}")
    public ResponseEntity<SerializedUnit> getUnitDetails(@PathVariable String serialNumber) {
        String safeSerialNumber = sanitizeStrict(serialNumber);
        Optional<SerializedUnit> unitOpt = serializedUnitRepository.findBySerialNumber(safeSerialNumber);

        return unitOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 2. GET ITEM-LEVEL QR CODE
     * Fetches the unit by serial number and encodes its cryptographic qrHash into the image.
     */
    @GetMapping(value = "/{serialNumber}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getUnitQRCode(@PathVariable String serialNumber) {
        String safeSerialNumber = sanitizeStrict(serialNumber);

        // Clean delegate
        byte[] qrCode = qrCodeService.generateUnitQrCode(safeSerialNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
    }

    /**
     * 3. DISPENSE UNIT (The "De-aggregation" Event)
     * Strictly validates against the database.
     */
    @PostMapping("/{serialNumber}/dispense")
    public ResponseEntity<?> dispenseUnit(@PathVariable String serialNumber) {
        String safeSerialNumber = sanitizeStrict(serialNumber);
        Optional<SerializedUnit> unitOpt = serializedUnitRepository.findBySerialNumber(safeSerialNumber);

        if (unitOpt.isEmpty()) {
            log.warn("SECURITY ALERT: Attempted to dispense unknown unit: {}", safeSerialNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Counterfeit Warning: Serial number not found in registry.");
        }

        SerializedUnit unit = unitOpt.get();

        if ("DISPENSED".equals(unit.getCurrentStatus())) {
            return ResponseEntity.badRequest().body("Unit is already dispensed.");
        }
        if ("RECALLED".equals(unit.getCurrentStatus())) {
            return ResponseEntity.badRequest().body("Cannot dispense a recalled unit.");
        }

        unit.setCurrentStatus("DISPENSED");
        serializedUnitRepository.save(unit);

        log.info("Unit {} successfully dispensed to patient.", safeSerialNumber);
        return ResponseEntity.ok().body("Unit " + safeSerialNumber + " status updated to DISPENSED.");
    }

    @PostMapping("/dispense")
    public ResponseEntity<?> dispenseUnitByHash(@RequestBody DispenseRequest request) {
        String safeHash = sanitizeStrict(request.getQrHash());

        // Find the unit using the cryptographic hash scanned from the QR code
        Optional<SerializedUnit> unitOpt = serializedUnitRepository.findByQrHash(safeHash);

        if (unitOpt.isEmpty()) {
            log.warn("SECURITY ALERT: Attempted to dispense unknown hash: {}", safeHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Counterfeit Warning: QR Hash not found in registry.");
        }

        SerializedUnit unit = unitOpt.get();

        if ("DISPENSED".equals(unit.getCurrentStatus())) {
            return ResponseEntity.badRequest().body("Unit is already dispensed.");
        }
        if ("RECALLED".equals(unit.getCurrentStatus())) {
            return ResponseEntity.badRequest().body("Cannot dispense a recalled unit.");
        }

        unit.setCurrentStatus("DISPENSED");
        serializedUnitRepository.save(unit);

        log.info("Unit {} (Hash: {}) successfully dispensed to patient.", unit.getSerialNumber(), safeHash);
        return ResponseEntity.ok().body("Medication dispensed successfully.");
    }

    private String sanitizeStrict(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}