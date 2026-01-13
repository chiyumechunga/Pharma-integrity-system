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
@RequiredArgsConstructor // Automatically injects all 'final' fields (cleaner code)
public class EventProcessingServiceImpl implements EventProcessingService {

    // 1. REPOSITORIES NEEDED
    private final PharmaceuticalRegistryRepository registryRepo;
    private final SupplyChainParticipantRepository participantRepo; // Needed to find manufacturer
    private final EventCheckpointRepository checkpointRepo;         // Needed for reliability
    private final FailedEventRepository failedEventRepo;            // Needed for error logging

    @Override
    @Transactional
    public void processBlockchainEvent(FireflyEventDto event) {
        String eventId = event.id();
        log.info("Processing Firefly Event ID: {}", eventId);

        // 2. SAFETY CHECK: Empty Data
        if (event.output() == null || event.output().data() == null) {
            log.warn("Received empty event payload. Ignoring.");
            return;
        }

        // 3. IDEMPOTENCY CHECK (Reliability)
        // If we processed this specific event ID before, stop immediately.
        if (checkpointRepo.existsById(eventId)) {
            log.info("Event {} already processed. Skipping.", eventId);
            return;
        }

        try {
            // 4. EXTRACT DATA
            var data = event.output().data();
            String qrHash = data.qrHash();

            // Check duplicate QR (Business Logic Idempotency)
            if (registryRepo.existsByQrHash(qrHash)) {
                log.info("Asset with QR Hash {} already exists in DB.", qrHash);
                saveCheckpoint(eventId, event.sequence()); // Mark as handled
                return;
            }

            // 5. THE BRIDGE: Convert ID String -> Entity Object
            // This fixes your compilation error.
            UUID manufacturerUuid = UUID.fromString(data.manufacturerId());
            SupplyChainParticipant manufacturer = participantRepo.findById(manufacturerUuid)
                    .orElseThrow(() -> new RuntimeException("Manufacturer not found with ID: " + manufacturerUuid));

            // 6. BUILD ENTITY
            PharmaceuticalRegistry entity = new PharmaceuticalRegistry();
            entity.setQrHash(qrHash);
            entity.setProductName(data.productName());
            entity.setBatchNumber(data.batchNumber());

            // KEY FIX: Setting the Object, not the ID
            entity.setManufacturer(manufacturer);

            entity.setExpiryDate(data.expiryDate());
            entity.setFireflyId(eventId);
            entity.setBlockchainTxId(event.transaction().id());
            entity.setCurrentStatus("ON_CHAIN"); // Confirmed status

            // 7. SAVE TO DB
            registryRepo.save(entity);

            // 8. SAVE CHECKPOINT (Success)
            saveCheckpoint(eventId, event.sequence());

            log.info("✅ projected Asset {} (Batch {}) to Database.", data.productName(), data.batchNumber());

        } catch (Exception e) {
            log.error("Failed to process blockchain event {}", eventId, e);

            // 9. FAULT TOLERANCE (Save failure for later retry)
            FailedEvent failure = new FailedEvent();
            failure.setTxId(event.transaction() != null ? event.transaction().id() : "UNKNOWN");
            failure.setRawPayload(event.toString());
            failure.setErrorMessage(e.getMessage());
            failedEventRepo.save(failure);

            // We do NOT throw the exception here.
            // We swallow it so the Controller returns 200 OK to Firefly.
            // Why? If we return 500, Firefly keeps retrying endlessly, jamming the queue.
            // We handle the error locally in 'failed_events' table instead.
        }
    }

    private void saveCheckpoint(String eventId, String sequence) {
        EventCheckpoint checkpoint = new EventCheckpoint();
        checkpoint.setListenerId(eventId);
        checkpoint.setLastEventSequence(sequence);
        checkpointRepo.save(checkpoint);
    }
}