package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.firefly.AssetData;
import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.*;
import com.chiyumechunga.backend.repository.*;
import com.chiyumechunga.backend.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingServiceImpl implements EventProcessingService {

    private final PharmaceuticalRegistryRepository registryRepo;
    private final SupplyChainParticipantRepository participantRepo;
    private final ChainOfCustodyRepository custodyRepo;
    private final EventCheckpointRepository checkpointRepo;
    private final FailedEventRepository failedEventRepo;
    private final RegulatoryScrutinyRepository scrutinyRepo;

    private String mapBlockchainStatus(String incomingStatus) {
        if (incomingStatus == null || "ON_CHAIN".equals(incomingStatus)) {
            return "CONFIRMED";
        }
        return incomingStatus;
    }

    @Override
    @Transactional
    public void processBlockchainEvent(FireflyEventDto event) {
        UUID eventId = event.id();
        log.info("Processing Firefly Event ID: {}", eventId);

        // FIX: Check inside the blockchainEvent object
        if (event.blockchainEvent() == null || event.blockchainEvent().output() == null) {
            log.warn("Received empty event payload. Ignoring.");
            return;
        }

        // Idempotency Check
        if (checkpointRepo.existsById(eventId.toString())) {
            log.info("Event {} already processed. Skipping.", eventId);
            return;
        }

        try {
            String eventName = event.blockchainEvent().name();

            // FIX: Extract data from inside the blockchainEvent object
            var data = event.blockchainEvent().output();

            switch (eventName) {
                case "AssetCreated":
                    handleAssetCreated(eventId, data, event);
                    break;
                case "CustodyTransferred":
                    handleCustodyTransferred(eventId, data, event);
                    break;
                default:
                    log.warn("Unknown blockchain event name: {}. Ignoring payload.", eventName);
            }

            saveCheckpoint(eventId.toString(), event.sequence());

        } catch (Exception e) {
            log.error("Failed to process blockchain event {}", eventId, e);

            FailedEvent failure = new FailedEvent();
            failure.setTxId(event.transaction() != null ? event.transaction().id() : "UNKNOWN");
            failure.setRawPayload(event.toString());
            failure.setErrorMessage(e.getMessage());
            failedEventRepo.save(failure);

            throw new RuntimeException("Event processing failed, routed to DLQ", e);
        }
    }

    private void handleAssetCreated(UUID eventId, AssetData data, FireflyEventDto event) {
        String qrHash = data.qrHash();
        String finalStatus = mapBlockchainStatus(data.currentStatus());

        Optional<PharmaceuticalRegistry> existingRecordOpt = registryRepo.findByQrHash(qrHash);

        if (existingRecordOpt.isPresent()) {
            PharmaceuticalRegistry existing = existingRecordOpt.get();

            if ("PENDING_BLOCKCHAIN".equals(existing.getCurrentStatus()) ||
                    "PENDING_CONFIRMATION".equals(existing.getCurrentStatus())) {

                existing.setCurrentStatus(finalStatus);
                existing.setConfirmedAt(ZonedDateTime.now().toLocalDateTime());

                if (event.transaction() != null) {
                    existing.setBlockchainTxId(event.transaction().id());
                }
                existing.setFireflyId(eventId);
                registryRepo.save(existing);
                log.info("Successfully confirmed Batch {} on the blockchain.", data.batchNumber());
            }
        } else {
            UUID manufacturerUuid = data.manufacturerId();
            SupplyChainParticipant manufacturer = participantRepo.findById(manufacturerUuid)
                    .orElseThrow(() -> new RuntimeException("Manufacturer not found: " + manufacturerUuid));

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

            entity.setCurrentStatus(finalStatus);
            entity.setConfirmedAt(ZonedDateTime.now().toLocalDateTime());
            registryRepo.save(entity);
            log.info("Successfully synchronized external Batch {} to local database.", data.batchNumber());
        }
    }

    private void handleCustodyTransferred(UUID eventId, AssetData data, FireflyEventDto event) {
        String qrHash = data.qrHash();

        PharmaceuticalRegistry registry = registryRepo.findByQrHash(qrHash)
                .orElseThrow(() -> new RuntimeException("Cannot transfer custody. Asset not found for QR: " + qrHash));

        SupplyChainParticipant fromParticipant = participantRepo.findById(data.fromParticipantId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + data.fromParticipantId()));

        SupplyChainParticipant toParticipant = participantRepo.findById(data.toParticipantId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + data.toParticipantId()));

        ChainOfCustodyEvent custodyEvent = new ChainOfCustodyEvent();

        // This will now work because we fixed the model class below!
        custodyEvent.setRegistry(registry);

        custodyEvent.setFromParticipant(fromParticipant);
        custodyEvent.setToParticipant(toParticipant);
        custodyEvent.setEventType(data.eventType());
        custodyEvent.setQuantity(data.quantity());

        if (event.transaction() != null) {
            custodyEvent.setBlockchainTxId(event.transaction().id());
        } else {
            custodyEvent.setBlockchainTxId(data.txId());
        }

        custodyRepo.save(custodyEvent);

        if ("DISPENSED".equals(data.eventType()) || "DESTROYED".equals(data.eventType())) {
            registry.setCurrentStatus(data.eventType());
            registryRepo.save(registry);
        }

        // Syntax error cleanly fixed here
        log.info("Successfully recorded custody transfer {} for QR {}. Sender: {}, Receiver: {}",
                data.eventType(), qrHash, fromParticipant.getParticipantCode(), toParticipant.getParticipantCode());
    }

    private void saveCheckpoint(String eventId, String sequence) {
        EventCheckpoint checkpoint = new EventCheckpoint();
        checkpoint.setListenerId(eventId);
        checkpoint.setLastEventSequence(sequence);
        checkpointRepo.save(checkpoint);
    }

    // --- TEST RESULTS LOGIC (USING REGULATORY SCRUTINY) ---
    private void handleTestResultsSubmitted(UUID eventId, AssetData data, FireflyEventDto event) {
        String qrHash = data.qrHash();

        PharmaceuticalRegistry registry = registryRepo.findByQrHash(qrHash)
                .orElseThrow(() -> new RuntimeException("Cannot log test results. Asset not found for QR: " + qrHash));

        // 1. Use your existing RegulatoryScrutiny model
        RegulatoryScrutiny scrutiny = new RegulatoryScrutiny();
        scrutiny.setRegistry(registry);
        scrutiny.setScrutinyDate(java.time.LocalDate.now());

        // Assuming data.currentStatus() string matches your TestResult Enum (e.g., "PASSED", "FAILED")
        try {
            scrutiny.setTestResult(com.chiyumechunga.backend.model.TestResult.valueOf(data.currentStatus()));
        } catch (IllegalArgumentException e) {
            log.warn("Could not map blockchain status '{}' to TestResult Enum.", data.currentStatus());
        }

        // Handle the transaction ID
        if (event.transaction() != null) {
            scrutiny.setBlockchainTxId(event.transaction().id());
        } else {
            scrutiny.setBlockchainTxId(data.txId() != null ? data.txId() : "UNKNOWN_TX");
        }

        // Note: inspectorId and labNotes might not be in the FireFly blockchain event payload.
        // If they aren't, they will safely remain null, or you can fetch them if you update AssetData later!

        scrutinyRepo.save(scrutiny);

        // 2. Update the main Registry table so the whole system knows the batch passed/failed
        registry.setCurrentStatus(data.currentStatus());
        //registry.setApprovedByZamra("PASSED".equalsIgnoreCase(data.currentStatus()));
        registryRepo.save(registry);

        log.info("Successfully recorded Regulatory Scrutiny for QR {}. Status: {}", qrHash, data.currentStatus());
    }
}