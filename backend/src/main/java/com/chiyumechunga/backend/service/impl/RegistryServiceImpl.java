package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private final FireflyIntegrationService fireflyService;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final SupplyChainParticipantRepository participantRepository;

    public RegistryServiceImpl(FireflyIntegrationService fireflyService,
                               PharmaceuticalRegistryRepository registryRepository,
                               SupplyChainParticipantRepository participantRepository) {
        this.fireflyService = fireflyService;
        this.registryRepository = registryRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * WORKFLOW: Two-Phase Batch Registration
     *
     * Phase 1: Create batch on blockchain WITHOUT QR hash
     * Phase 2: Generate QR hash using blockchain tx_id
     * Phase 3: Attach QR hash back to blockchain
     * Phase 4: Confirm in Postgres
     */
    @Override
    @Transactional
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("🔵 Phase 1: Creating batch '{}' on blockchain (WITHOUT QR hash)", request.batchNumber());

        // 1. Validate manufacturer exists
        SupplyChainParticipant manufacturer = participantRepository.findById(request.manufacturerId())
                .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + request.manufacturerId()));

        // 2. Check for duplicate batch number
        if (registryRepository.findByBatchNumber(request.batchNumber()).isPresent()) {
            throw new DuplicateResourceException("Batch number already exists: " + request.batchNumber());
        }

        // 3. Create initial Postgres record (PENDING_BLOCKCHAIN status)
        PharmaceuticalRegistry registry = new PharmaceuticalRegistry();
        registry.setProductName(request.productName());
        registry.setBatchNumber(request.batchNumber());
        registry.setManufacturer(manufacturer);
        registry.setManufacturingDate(request.manufacturingDate());
        registry.setExpiryDate(request.expiryDate());
        registry.setCurrentStatus("PENDING_BLOCKCHAIN");
        registry.setQrHash(""); // Empty initially

        PharmaceuticalRegistry savedRegistry = registryRepository.save(registry);
        log.info("✅ Postgres record created with ID: {}", savedRegistry.getRegistryId());

        // 4. Invoke blockchain (Phase 1: CreateAsset WITHOUT QR hash)
        String operationId = fireflyService.invokeContract(
                "CreateAsset",
                createBlockchainPayload(request, manufacturer),
                ParticipantType.MANUFACTURER
        );

        log.info("✅ Blockchain invocation initiated. Operation ID: {}", operationId);

        // 5. Async Phase 2 & 3: Wait for blockchain confirmation, then generate & attach QR hash
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: In production, poll Firefly for operation status
                // For now, simulate a delay for blockchain confirmation
                Thread.sleep(3000); // 3 seconds

                // Simulate getting tx_id from Firefly (in production, query /operations/{id})
                String blockchainTxId = "tx-" + System.currentTimeMillis(); // Mock tx_id

                // Generate QR Hash
                String qrHash = generateQRHash(
                        request.batchNumber(),
                        blockchainTxId,
                        request.productName(),
                        request.expiryDate().toString()
                );

                log.info("🟢 Phase 2: Generated QR Hash: {}", qrHash);

                // Update Postgres with blockchain tx_id and QR hash
                savedRegistry.setBlockchainTxId(blockchainTxId);
                savedRegistry.setQrHash(qrHash);
                savedRegistry.setCurrentStatus("PENDING_CONFIRMATION");
                registryRepository.save(savedRegistry);

                // Phase 3: Attach QR hash to blockchain
                fireflyService.invokeContract(
                        "AttachQRHash",
                        createAttachQRHashPayload(request.batchNumber(), qrHash, savedRegistry.getRegistryId().toString()),
                        ParticipantType.MANUFACTURER
                );

                log.info("🟢 Phase 3: QR hash attached to blockchain for batch: {}", request.batchNumber());

                // Phase 4: Mark as CONFIRMED (this will be updated by event listener in production)
                savedRegistry.setCurrentStatus("CONFIRMED");
                savedRegistry.setConfirmedAt(LocalDateTime.now());
                registryRepository.save(savedRegistry);

                log.info("✅ Batch registration complete! QR Hash: {}", qrHash);

            } catch (Exception e) {
                log.error("❌ Failed to complete QR hash workflow for batch {}", request.batchNumber(), e);
                savedRegistry.setCurrentStatus("BLOCKCHAIN_FAILED");
                registryRepository.save(savedRegistry);
            }
        });

        return new FireflyAckDto(
                operationId,
                "PENDING_CONFIRMATION",
                "Batch registration initiated. QR hash will be generated after blockchain confirmation."
        );
    }

    /**
     * Generate deterministic QR hash using SHA-256.
     */
    private String generateQRHash(String batchNumber, String txId,
                                  String productName, String expiryDate) {

        String concatenated = batchNumber + txId + productName + expiryDate;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string (64 characters)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Create payload for blockchain CreateAsset call (without QR hash).
     * Uses Map to avoid anonymous class shadowing issues.
     */
    private Map<String, Object> createBlockchainPayload(RegistryRequestDto request, SupplyChainParticipant manufacturer) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchNumber", request.batchNumber());
        payload.put("productId", "product-" + request.productName());
        payload.put("productName", request.productName());
        payload.put("manufacturerId", manufacturer.getParticipantId().toString());
        payload.put("manufacturerMspId", "ManufacturerMSP");
        payload.put("manufacturingDate", request.manufacturingDate() != null
                ? request.manufacturingDate().toString()
                : LocalDateTime.now().toLocalDate().toString());
        payload.put("expiryDate", request.expiryDate().toString());
        return payload;
    }

    /**
     * Create payload for blockchain AttachQRHash call.
     * Uses Map to avoid anonymous class shadowing issues.
     */
    private Map<String, Object> createAttachQRHashPayload(String batchNumber, String qrHash, String registryId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchNumber", batchNumber);
        payload.put("qrHash", qrHash);
        payload.put("registryId", registryId);
        return payload;
    }
}