package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private final ObjectMapper objectMapper;
    private final FireflyIntegrationService fireflyService;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final SupplyChainParticipantRepository participantRepository;

    public RegistryServiceImpl(FireflyIntegrationService fireflyService,
                               PharmaceuticalRegistryRepository registryRepository,
                               SupplyChainParticipantRepository participantRepository,
                               ObjectMapper objectMapper) {
        this.fireflyService = fireflyService;
        this.registryRepository = registryRepository;
        this.participantRepository = participantRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("🔵 Phase 1: Initiating Batch Registration for '{}'", request.batchNumber());

        // 1. ZAMRA REGULATORY VALIDATION
        if (request.expiryDate().isBefore(LocalDate.now().plusMonths(6))) {
            throw new IllegalArgumentException("ZAMRA Constraint: Expiry must be at least 6 months in the future.");
        }

        // 2. VALIDATE PARTICIPANT & UNIQUE BATCH
        SupplyChainParticipant manufacturer = participantRepository.findById(request.manufacturerId())
                .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + request.manufacturerId()));

        if (registryRepository.findByBatchNumber(request.batchNumber()).isPresent()) {
            throw new DuplicateResourceException("Batch number already exists: " + request.batchNumber());
        }

        // 3. STAGE DATABASE RECORD (Intent Phase)
        PharmaceuticalRegistry registry = new PharmaceuticalRegistry();
        registry.setProductName(request.productName());
        registry.setBatchNumber(request.batchNumber());
        registry.setManufacturer(manufacturer);
        registry.setManufacturingDate(request.manufacturingDate());
        registry.setExpiryDate(request.expiryDate());
        registry.setBlockchainTxId("tx-pending-" + UUID.randomUUID());
        String pendingQrHash = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        registry.setQrHash(pendingQrHash);
        registry.setCurrentStatus("PENDING_BLOCKCHAIN");

        // 4. PERSIST INTENT
        registryRepository.save(registry);
        log.info("✅ Database intent staged for batch {}", request.batchNumber());

        // 5. BLOCKCHAIN PHASE 1: CreateAsset
        String rawResponse = fireflyService.invokeContract(
                "CreateAsset",
                createBlockchainPayload(request, manufacturer),
                ParticipantType.MANUFACTURER
        );
        log.info("🔗 FireFly raw response: {}", rawResponse);

        // 6. PARSE FIREFLY RESPONSE & UPDATE TX ID
        String resolvedOperationId = rawResponse; // fallback
        try {
            Map<String, Object> ffResponse = objectMapper.readValue(rawResponse, Map.class);
            String realTxId = (String) ffResponse.get("id");
            if (realTxId != null) {
                registry.setBlockchainTxId(realTxId);
                registry.setFireflyId(UUID.fromString(realTxId)); // ADD THIS
                registry.setCurrentStatus("CONFIRMED");
                registryRepository.save(registry);
            } else {
                log.warn("⚠️ FireFly response had no 'id' field: {}", rawResponse);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not parse FireFly response — raw: {}", rawResponse);
        }

        return new FireflyAckDto(
                resolvedOperationId,
                "PENDING_CONFIRMATION",
                "Registration initiated. QR hash will be generated upon blockchain consensus."
        );
    }

    @Override
    public PharmaceuticalRegistry getBatchDetails(String batchNumber) {
        return registryRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchNumber));
    }

    private Map<String, Object> createBlockchainPayload(RegistryRequestDto request, SupplyChainParticipant manufacturer) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchNumber", request.batchNumber());
        payload.put("productId", "PROD-" + request.productName().toUpperCase());
        payload.put("productName", request.productName());
        payload.put("manufacturerId", manufacturer.getParticipantId().toString());
        payload.put("manufacturerMspId", "Org1MSP");
        payload.put("manufacturingDate", request.manufacturingDate().toString());
        payload.put("expiryDate", request.expiryDate().toString());
        return payload;
    }
}
