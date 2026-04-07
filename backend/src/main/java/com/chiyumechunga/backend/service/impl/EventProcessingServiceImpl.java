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

import java.util.Optional;
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
        UUID eventId = event.id();
        log.info("Processing Firefly Event ID: {}", eventId);

        if (event.output() == null || event.output().data() == null) {
            log.warn("Received empty event payload. Ignoring.");
            return;
        }

        // Idempotency: Have we processed this exact FireFly event before?
        if (checkpointRepo.existsById(eventId.toString())) {
            log.info("Event {} already processed. Skipping.", eventId);
            return;
        }

        try {
            var data = event.output().data();
            String qrHash = data.qrHash();
            String eventType = event.type(); // Usually "AssetCreated", "CustodyTransferred", etc.

            // 1. THE BRIDGE: Connect the Webhook to the RegistryServiceImpl
            Optional<PharmaceuticalRegistry> existingRecordOpt = registryRepo.findByQrHash(qrHash);

            if (existingRecordOpt.isPresent()) {
                PharmaceuticalRegistry existing = existingRecordOpt.get();

                // If the record was created by RegistryServiceImpl and is waiting for blockchain confirmation
                if ("PENDING_BLOCKCHAIN".equals(existing.getCurrentStatus()) ||
                        "PENDING_CONFIRMATION".equals(existing.getCurrentStatus())) {

                    log.info("Pending batch {} found. Upgrading status to ON_CHAIN.", data.batchNumber());

                    existing.setCurrentStatus("ON_CHAIN");
                    if (event.transaction() != null) {
                        existing.setBlockchainTxId(event.transaction().id());
                    }
                    existing.setFireflyId(eventId);

                    registryRepo.save(existing);
                    log.info(" Successfully confirmed Batch {} on the blockchain.", data.batchNumber());
                } else {
                    log.info("Batch with QR Hash {} already exists and is in status {}. Skipping creation.",
                            qrHash, existing.getCurrentStatus());
                }
            } else {
                // 2. FALLBACK: If the asset was created on a different FireFly node and we are just hearing about it
                log.info("New asset detected from network. Projecting Asset {} to Database.", data.batchNumber());

                UUID manufacturerUuid = data.manufacturerId();
                SupplyChainParticipant manufacturer = participantRepo.findById(manufacturerUuid)
                        .orElseThrow(() -> new RuntimeException("Manufacturer not found with ID: " + manufacturerUuid));

                PharmaceuticalRegistry entity = new PharmaceuticalRegistry();
                entity.setQrHash(qrHash);
                entity.setProductName(data.productName());
                entity.setBatchNumber(data.batchNumber());
                entity.setManufacturer(manufacturer);
                entity.setExpiryDate(data.expiryDate());
                entity.setFireflyId(eventId);

                if (event.transaction() != null) {
                    entity.setBlockchainTxId(event.transaction().id());
                }

                entity.setCurrentStatus("ON_CHAIN");
                registryRepo.save(entity);
                log.info(" Successfully synchronized external Batch {} to local database.", data.batchNumber());
            }

            // 3. Mark event as completed to prevent duplicate processing
            saveCheckpoint(eventId.toString(), event.sequence());

        } catch (Exception e) {
            log.error("Failed to process blockchain event {}", eventId, e);

            // 4. Dead Letter Queue Integration
            FailedEvent failure = new FailedEvent();
            failure.setTxId(event.transaction() != null ? event.transaction().id() : "UNKNOWN");
            failure.setRawPayload(event.toString());
            failure.setErrorMessage(e.getMessage());
            failedEventRepo.save(failure);

            // Re-throw to let the controller handle it if needed, or let it be swallowed
            // since the controller already has a DLQ catch block.
            throw new RuntimeException("Event processing failed, routed to DLQ", e);
        }
    }

    private void saveCheckpoint(String eventId, String sequence) {
        EventCheckpoint checkpoint = new EventCheckpoint();
        checkpoint.setListenerId(eventId);
        checkpoint.setLastEventSequence(sequence);
        checkpointRepo.save(checkpoint);
    }
}