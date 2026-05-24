package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.exception.QrCodeGenerationException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SerializedUnitRepository;
import com.chiyumechunga.backend.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final SerializedUnitRepository unitRepository;

    private static final int DEFAULT_SIZE = 250;

    @Override
    public String generateBatchQrHash(String batchNumber, UUID productId, UUID manufacturerId) {
        return generateHash(batchNumber, productId, manufacturerId);
    }

    @Override
    public String generateUnitQrHash(String serialNumber, UUID productId, UUID manufacturerId) {
        return generateHash(serialNumber, productId, manufacturerId);
    }

    private String generateHash(String identifier, UUID productId, UUID manufacturerId) {
        try {
            String rawData = identifier + "|" + productId + "|" + manufacturerId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new IllegalStateException("Missing SHA-256 algorithm required for hashing", e);
        }
    }

    @Override
    public byte[] generateQrCodeImage(String payload, int width, int height) {
        try {
            QRCodeWriter barcodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = barcodeWriter.encode(payload, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code image for payload: {}", payload, e);
            throw new QrCodeGenerationException("Failed to render QR Code image", e);
        }
    }

    @Override
    public byte[] generateBatchQrCode(String batchNumber) {
        // Find the batch. If not found, throw exception for global handler to catch (404)
        PharmaceuticalRegistry batch = registryRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with number: " + batchNumber));

        return generateQrCodeImage(batch.getQrHash(), DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Override
    public byte[] generateUnitQrCode(String serialNumber) {
        // Find the unit. If not found, throw exception for global handler to catch (404)
        SerializedUnit unit = unitRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with serial number: " + serialNumber));

        return generateQrCodeImage(unit.getQrHash(), DEFAULT_SIZE, DEFAULT_SIZE);
    }
}