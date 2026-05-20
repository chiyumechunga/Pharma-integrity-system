package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductMaster;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.ProductMasterRepository;
import com.chiyumechunga.backend.repository.SerializedUnitRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private final ObjectMapper objectMapper;
    private final FireflyIntegrationService fireflyService;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final SupplyChainParticipantRepository participantRepository;
    private final ProductMasterRepository productMasterRepository;
    private final SerializedUnitRepository serializedUnitRepository;

    public RegistryServiceImpl(FireflyIntegrationService fireflyService,
                               PharmaceuticalRegistryRepository registryRepository,
                               SupplyChainParticipantRepository participantRepository,
                               ProductMasterRepository productMasterRepository,
                               SerializedUnitRepository serializedUnitRepository,
                               ObjectMapper objectMapper) {
        this.fireflyService = fireflyService;
        this.registryRepository = registryRepository;
        this.participantRepository = participantRepository;
        this.productMasterRepository = productMasterRepository;
        this.serializedUnitRepository = serializedUnitRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalRegistry> getAllBatches() {
        return registryRepository.findAll();
    }

    @Override
    @Transactional
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("Starting batch registration for batchNumber={}", request.batchNumber());

        if (request.expiryDate().isBefore(LocalDate.now().plusMonths(6))) {
            throw new IllegalArgumentException("ZAMRA Constraint: Expiry date must be at least 6 months in the future.");
        }

        ProductMaster product = productMasterRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        int declaredUnits = request.batchUnitCount();

        if (declaredUnits > product.getMaxUnitsPerBatch()) {
            throw new IllegalArgumentException("Validation Failed: Batch unit count exceeds maximum.");
        }

        SupplyChainParticipant manufacturer = participantRepository.findById(request.manufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));

        if (manufacturer.getRole() != ParticipantType.MANUFACTURER) {
            throw new IllegalArgumentException("Participant is not a MANUFACTURER.");
        }

        if (registryRepository.findByBatchNumber(request.batchNumber()).isPresent()) {
            throw new DuplicateResourceException("Batch number already exists: " + request.batchNumber());
        }

        String qrHash = generateQrHash(request.batchNumber(), product.getProductId().toString(), manufacturer.getParticipantId().toString());

        PharmaceuticalRegistry registry = new PharmaceuticalRegistry();
        registry.setProduct(product);
        registry.setProductName(product.getGenericName());
        registry.setBatchNumber(request.batchNumber());
        registry.setBatchUnitCount(declaredUnits);
        registry.setManufacturer(manufacturer);
        registry.setManufacturingDate(request.manufacturingDate());
        registry.setExpiryDate(request.expiryDate());
        registry.setQrHash(qrHash);
        registry.setBlockchainTxId("pending-" + UUID.randomUUID().toString().replace("-", ""));
        registry.setCurrentStatus("PENDING_BLOCKCHAIN");

        PharmaceuticalRegistry savedBatch = registryRepository.save(registry);

        // -----------------------------------------------------------------
        // STEP 7: THE EXPLOSION LOGIC (Item-Level Serialization)
        // -----------------------------------------------------------------
        List<SerializedUnit> unitsToSave = new ArrayList<>();
        for (int i = 1; i <= declaredUnits; i++) {
            SerializedUnit unit = new SerializedUnit();
            unit.setRegistryId(savedBatch.getRegistryId());

            String serialNumber = String.format("%s-SN%03d", savedBatch.getBatchNumber(), i);
            unit.setSerialNumber(serialNumber);

            // Generate and save the unique item-level hash
            unit.setQrHash(generateQrHash(serialNumber, product.getProductId().toString(), manufacturer.getParticipantId().toString()));

            unit.setCurrentStatus("IN_BATCH");
            unitsToSave.add(unit);
        }

        serializedUnitRepository.saveAll(unitsToSave);

        String rawResponse = fireflyService.invokeContract(
                "CreateAsset",
                createBlockchainPayload(request, product, manufacturer, declaredUnits),
                ParticipantType.MANUFACTURER
        );

        String resolvedOperationId = savedBatch.getBlockchainTxId();
        try {
            Map<String, Object> ffResponse = objectMapper.readValue(rawResponse, Map.class);
            String realTxId = (String) ffResponse.get("id");

            if (realTxId != null && !realTxId.isBlank()) {
                savedBatch.setBlockchainTxId(realTxId);
                savedBatch.setFireflyId(UUID.fromString(realTxId));
                savedBatch.setCurrentStatus("PENDING_CONFIRMATION");
                registryRepository.save(savedBatch);
                resolvedOperationId = realTxId;
            }
        } catch (Exception e) {
            log.warn("Failed to parse FireFly response; batch remains PENDING_BLOCKCHAIN.");
        }

        return new FireflyAckDto(resolvedOperationId, "PENDING_CONFIRMATION", "Batch and " + declaredUnits + " serialized units created.");
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalRegistry getBatchDetails(String batchNumber) {
        return registryRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchNumber));
    }

    private String generateQrHash(String batchNumber, String productId, String manufacturerId) {
        try {
            String input = batchNumber + "|" + productId + "|" + manufacturerId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) { sb.append(String.format("%02x", b)); }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR hash", e);
        }
    }

    private Map<String, Object> createBlockchainPayload(RegistryRequestDto request, ProductMaster product, SupplyChainParticipant manufacturer, int declaredUnits) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchNumber", request.batchNumber());
        payload.put("batchUnitCount", declaredUnits);
        payload.put("productId", product.getProductId().toString());
        payload.put("productName", product.getGenericName());
        payload.put("productCode", product.getProductCode());
        payload.put("brandName", product.getBrandName());
        payload.put("dosageForm", product.getDosageForm());
        payload.put("strength", product.getStrength());
        payload.put("therapeuticClass", product.getTherapeuticClass());
        payload.put("requiresColdChain", product.isRequiresColdChain());
        payload.put("approvedByZamra", product.isApprovedByZamra());
        payload.put("manufacturerId", manufacturer.getParticipantId().toString());
        payload.put("manufacturerName", manufacturer.getParticipantName());
        payload.put("manufacturerCode", manufacturer.getParticipantCode());
        payload.put("manufacturerCountry", manufacturer.getCountry());
        payload.put("manufacturingDate", request.manufacturingDate() != null ? request.manufacturingDate().toString() : null);
        payload.put("expiryDate", request.expiryDate().toString());
        payload.put("qrHash", generateQrHash(request.batchNumber(), product.getProductId().toString(), manufacturer.getParticipantId().toString()));
        return payload;
    }
}