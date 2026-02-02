package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.EventCheckpoint;
import com.chiyumechunga.backend.model.FailedEvent;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.EventCheckpointRepository;
import com.chiyumechunga.backend.repository.FailedEventRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingServiceImpl implements EventProcessingService {

    private final PharmaceuticalRegistryRepository registryRepo;
    private final SupplyChainParticipantRepository participantRepo;
    private final EventCheckpointRepository checkpointRepo;
    private final FailedEventRepository failedEventRepo;

    @Override
    @Transactional
    public void processBlockchainEvent(FireflyEventDto event) {
        // event.id() is already a UUID based on your updated DTO
        UUID eventId = event.id();
        log.info("Processing Firefly Event ID: {}", eventId);

        // 2. SAFETY CHECK
        if (event.output() == null || event.output().data() == null) {
            log.warn("Received empty event payload. Ignoring.");
            return;
        }

        // 3. IDEMPOTENCY CHECK
        // FIX 1: Convert UUID -> String because Checkpoint ID is VARCHAR in DB
        if (checkpointRepo.existsById(eventId.toString())) {
            log.info("Event {} already processed. Skipping.", eventId);
            return;
        }

        try {
            // 4. EXTRACT DATA
            var data = event.output().data();
            String qrHash = data.qrHash();

            // Business Logic Idempotency
            if (registryRepo.existsByQrHash(qrHash)) {
                log.info("Asset with QR Hash {} already exists in DB.", qrHash);
                // FIX 2: Convert UUID -> String for the helper method
                saveCheckpoint(eventId.toString(), event.sequence());
                return;
            }

            // 5. THE BRIDGE
            // FIX 3: Removed UUID.fromString() because manufacturerId() is ALREADY a UUID
            UUID manufacturerUuid = data.manufacturerId();

            SupplyChainParticipant manufacturer = participantRepo.findById(manufacturerUuid)
                    .orElseThrow(() -> new RuntimeException("Manufacturer not found with ID: " + manufacturerUuid));

            // 6. BUILD ENTITY
            PharmaceuticalRegistry entity = new PharmaceuticalRegistry();
            entity.setQrHash(qrHash);
            entity.setProductName(data.productName());
            entity.setBatchNumber(data.batchNumber());
            entity.setManufacturer(manufacturer);
            entity.setExpiryDate(data.expiryDate());
            entity.setFireflyId(eventId);

            // Handle potentially null transaction
            if (event.transaction() != null) {
                entity.setBlockchainTxId(event.transaction().id());
            }

            entity.setCurrentStatus("ON_CHAIN");

            // 7. SAVE TO DB
            registryRepo.save(entity);

            // 8. SAVE CHECKPOINT
            // FIX 4: Convert UUID -> String
            saveCheckpoint(eventId.toString(), event.sequence());

            log.info("✅ Projected Asset {} (Batch {}) to Database.", data.productName(), data.batchNumber());

        } catch (Exception e) {
            log.error("Failed to process blockchain event {}", eventId, e);

            // 9. FAULT TOLERANCE
            FailedEvent failure = new FailedEvent();
            failure.setTxId(event.transaction() != null ? event.transaction().id() : "UNKNOWN");
            failure.setRawPayload(event.toString());
            failure.setErrorMessage(e.getMessage());
            failedEventRepo.save(failure);
        }
    }

    private void saveCheckpoint(String eventId, String sequence) {
        EventCheckpoint checkpoint = new EventCheckpoint();
        checkpoint.setListenerId(eventId);
        checkpoint.setLastEventSequence(sequence);
        checkpointRepo.save(checkpoint);
    }
}