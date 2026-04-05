package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductMaster;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.ProductMasterRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation responsible for writing rows into pharmaceutical_registry
 * and initiating the corresponding blockchain transaction through FireFly.
 *
 * This class contains the major corrections needed for the original implementation:
 *
 * 1. product_master must be resolved before batch creation
 *    - original code used only productName and ignored product_id
 *    - corrected code resolves ProductMaster using RegistryRequestDto.productId
 *
 * 2. pharmaceutical_registry.product_id must be populated
 *    - original code never set the FK
 *    - corrected code calls registry.setProduct(product)
 *
 * 3. product_name must come from product_master.generic_name
 *    - original code accepted free text from the request
 *    - corrected code uses the authoritative catalog value
 *
 * 4. qr_hash must satisfy the database regex constraint
 *    - schema requires exactly 64 hex characters
 *    - original UUID concatenation approach did not satisfy the check
 *    - corrected code uses SHA-256 and returns a 64-char hex string
 *
 * 5. blockchain payload must use the real product UUID
 *    - original code fabricated a productId like PROD-AMOXICILLIN
 *    - corrected code uses product.getProductId().toString()
 *
 * 6. status progression must match actual lifecycle
 *    - original code moved to CONFIRMED too early
 *    - corrected code uses PENDING_BLOCKCHAIN then PENDING_CONFIRMATION
 *    - final CONFIRMED should be set by the webhook/event confirmation layer
 */
@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private final ObjectMapper objectMapper;
    private final FireflyIntegrationService fireflyService;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final SupplyChainParticipantRepository participantRepository;
    private final ProductMasterRepository productMasterRepository;

    public RegistryServiceImpl(FireflyIntegrationService fireflyService,
                               PharmaceuticalRegistryRepository registryRepository,
                               SupplyChainParticipantRepository participantRepository,
                               ProductMasterRepository productMasterRepository,
                               ObjectMapper objectMapper) {
        this.fireflyService = fireflyService;
        this.registryRepository = registryRepository;
        this.participantRepository = participantRepository;
        this.productMasterRepository = productMasterRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registers a pharmaceutical batch.
     *
     * Correct process:
     * 1. validate request business rules
     * 2. resolve product from product_master using productId
     * 3. resolve manufacturer participant
     * 4. ensure participant role is MANUFACTURER
     * 5. ensure batch number is unique
     * 6. generate qr_hash in the exact format required by the DB
     * 7. write intent row to pharmaceutical_registry with PENDING_BLOCKCHAIN
     * 8. invoke FireFly contract using real identifiers
     * 9. parse FireFly response and move row to PENDING_CONFIRMATION
     * 10. wait for external confirmation flow to mark CONFIRMED
     */
    @Override
    @Transactional
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("Starting batch registration for batchNumber={}", request.batchNumber());

        // -----------------------------------------------------------------
        // STEP 1: Validate expiry against the same business rule as the schema
        // -----------------------------------------------------------------
        if (request.expiryDate().isBefore(LocalDate.now().plusMonths(6))) {
            throw new IllegalArgumentException(
                    "ZAMRA Constraint: Expiry date must be at least 6 months in the future.");
        }

        // -----------------------------------------------------------------
        // STEP 2: Resolve the product from product_master
        // -----------------------------------------------------------------
        ProductMaster product = productMasterRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.productId()
                                + ". Register the product first via POST /api/v1/products"));

        // -----------------------------------------------------------------
        // STEP 3: Resolve the manufacturer participant
        // -----------------------------------------------------------------
        SupplyChainParticipant manufacturer = participantRepository.findById(request.manufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manufacturer not found: " + request.manufacturerId()));

        // -----------------------------------------------------------------
        // STEP 4: Enforce that only MANUFACTURER can create a batch
        // -----------------------------------------------------------------
        if (manufacturer.getRole() != ParticipantType.MANUFACTURER) {
            throw new IllegalArgumentException(
                    "Participant " + request.manufacturerId()
                            + " is not a MANUFACTURER. Actual role: " + manufacturer.getRole());
        }

        // -----------------------------------------------------------------
        // STEP 5: Enforce unique batch number
        // -----------------------------------------------------------------
        if (registryRepository.findByBatchNumber(request.batchNumber()).isPresent()) {
            throw new DuplicateResourceException(
                    "Batch number already exists: " + request.batchNumber());
        }

        // -----------------------------------------------------------------
        // STEP 6: Generate qr_hash in the exact DB-accepted format
        // -----------------------------------------------------------------
        String qrHash = generateQrHash(
                request.batchNumber(),
                product.getProductId().toString(),
                manufacturer.getParticipantId().toString()
        );

        // -----------------------------------------------------------------
        // STEP 7: Create the DB intent row
        // -----------------------------------------------------------------
        PharmaceuticalRegistry registry = new PharmaceuticalRegistry();

        registry.setProduct(product);
        registry.setProductName(product.getGenericName());
        registry.setBatchNumber(request.batchNumber());
        registry.setManufacturer(manufacturer);
        registry.setManufacturingDate(request.manufacturingDate());
        registry.setExpiryDate(request.expiryDate());
        registry.setQrHash(qrHash);

        registry.setBlockchainTxId("pending-" + UUID.randomUUID().toString().replace("-", ""));
        registry.setCurrentStatus("PENDING_BLOCKCHAIN");

        registryRepository.save(registry);
        log.info("Batch intent saved to pharmaceutical_registry with status=PENDING_BLOCKCHAIN");

        // -----------------------------------------------------------------
        // STEP 8: Invoke blockchain contract using real product and manufacturer values
        // -----------------------------------------------------------------
        String rawResponse = fireflyService.invokeContract(
                "CreateAsset",
                createBlockchainPayload(request, product, manufacturer),
                ParticipantType.MANUFACTURER
        );

        log.info("FireFly response received: {}", rawResponse);

        // -----------------------------------------------------------------
        // STEP 9: Parse FireFly response
        // -----------------------------------------------------------------
        String resolvedOperationId = registry.getBlockchainTxId();

        try {
            Map<String, Object> ffResponse = objectMapper.readValue(rawResponse, Map.class);
            String realTxId = (String) ffResponse.get("id");

            if (realTxId != null && !realTxId.isBlank()) {
                registry.setBlockchainTxId(realTxId);
                registry.setFireflyId(UUID.fromString(realTxId));
                registry.setCurrentStatus("PENDING_CONFIRMATION");
                registryRepository.save(registry);
                resolvedOperationId = realTxId;

                log.info("Batch promoted to PENDING_CONFIRMATION with FireFly id={}", realTxId);
            } else {
                log.warn("FireFly response did not include an 'id' field; batch remains PENDING_BLOCKCHAIN");
            }
        } catch (Exception e) {
            log.warn("Failed to parse FireFly response; batch remains PENDING_BLOCKCHAIN. Reason={}", e.getMessage());
        }

        // -----------------------------------------------------------------
        // STEP 10: Return acknowledgment to caller
        // -----------------------------------------------------------------
        return new FireflyAckDto(
                resolvedOperationId,
                "PENDING_CONFIRMATION",
                "Batch registration submitted. Final CONFIRMED status should be set by blockchain event confirmation."
        );
    }

    /**
     * Retrieves a batch using its batch number.
     */
    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalRegistry getBatchDetails(String batchNumber) {
        return registryRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchNumber));
    }

    /**
     * Generates a SHA-256 hash and returns it as a 64-character lowercase hex string.
     *
     * Why this matters:
     * The schema enforces a regex requiring exactly 64 hexadecimal characters.
     * SHA-256 is therefore a natural and reliable choice.
     */
    private String generateQrHash(String batchNumber, String productId, String manufacturerId) {
        try {
            String input = batchNumber + "|" + productId + "|" + manufacturerId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR hash for batch: " + batchNumber, e);
        }
    }

    /**
     * Builds the payload sent to the blockchain contract.
     *
     * Major correction here:
     * The original code fabricated productId using product name.
     * This corrected version uses the real UUID from product_master.
     */
    private Map<String, Object> createBlockchainPayload(
            RegistryRequestDto request,
            ProductMaster product,
            SupplyChainParticipant manufacturer) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("batchNumber", request.batchNumber());

        // Correct: use actual catalog UUID
        payload.put("productId", product.getProductId().toString());

        // Correct: use authoritative catalog name
        payload.put("productName", product.getGenericName());

        // Helpful additional product metadata
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
        payload.put("qrHash", generateQrHash(
                request.batchNumber(),
                product.getProductId().toString(),
                manufacturer.getParticipantId().toString()));

        return payload;
    }
}