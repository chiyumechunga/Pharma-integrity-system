package com.chiyumechunga.backend.service;

import java.util.UUID;

public interface QrCodeService {
    String generateBatchQrHash(String batchNumber, UUID productId, UUID manufacturerId);
    String generateUnitQrHash(String serialNumber, UUID productId, UUID manufacturerId);

    byte[] generateQrCodeImage(String payload, int width, int height);
    byte[] generateBatchQrCode(String batchNumber);
    byte[] generateUnitQrCode(String serialNumber);
}