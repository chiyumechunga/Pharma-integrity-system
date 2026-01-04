package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class EventProcessingServiceImpl implements EventProcessingService {

    private final PharmaceuticalRegistryRepository repository;

    public EventProcessingServiceImpl(PharmaceuticalRegistryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void processBlockchainEvent(FireflyEventDto event) {
        log.info("Processing Firefly Event ID: {}", event.id());

        // 1. SAFETY CHECK: Ensure the event actually contains data
        if (event.output() == null || event.output().data() == null) {
            log.warn("Received empty event payload. Ignoring.");
            return;
        }

        var data = event.output().data();
        String qrHash = data.qrHash();

        // 2. IDEMPOTENCY CHECK (Crucial for Blockchain Events)
        // If we already have this QR hash confirmed ON_CHAIN, we skip to avoid duplicates.
        if (repository.existsByQrHash(qrHash)) {
            log.info("Asset with QR Hash {} already exists. Skipping duplicate event.", qrHash);
            return;
        }

        try {
            // 3. MAP DATA (Blockchain -> PostgreSQL)
            // We create the entity based purely on the "Truth" received from the chain.
            PharmaceuticalRegistry entity = new PharmaceuticalRegistry();

            // Business Data
            entity.setQrHash(qrHash);
            entity.setProductName(data.productName());
            entity.setBatchNumber(data.batchNumber());
            // Note: Manufacturer ID comes as String from JSON, assuming UUID format
            entity.setManufacturerId(UUID.fromString(data.manufacturerId()));
            entity.setExpiryDate(data.expiryDate());

            // Protocol Data (The Cryptographic Proof)
            entity.setFireflyId(event.id());
            entity.setBlockchainTxId(event.transaction().id()); //
            entity.setCurrentStatus("ON_CHAIN"); //

            // Timestamps
            // We use the server time for 'confirmedAt', or you could parse a block timestamp if available
            // entity.setConfirmedAt(LocalDateTime.now());

            // 4. PERSIST
            repository.save(entity);
            log.info("Successfully projected Asset {} to Database. Tx: {}",
                    data.productName(), event.transaction().id());

        } catch (Exception e) {
            log.error("Failed to process blockchain event for QR: {}", qrHash, e);
            // In a real system, you might send this to a "Dead Letter Queue" for manual review
            throw new RuntimeException("Event Processing Failed", e);
        }
    }
}