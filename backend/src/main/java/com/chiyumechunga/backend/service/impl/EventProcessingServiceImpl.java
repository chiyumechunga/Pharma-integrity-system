package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.firefly.AssetData;
import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.*;
import com.chiyumechunga.backend.repository.*;
import com.chiyumechunga.backend.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingServiceImpl implements EventProcessingService {

    private final SerializedUnitRepository unitRepo;
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
            var data = event.blockchainEvent().output();

            switch (eventName) {
                case "AssetCreated":
                    handleAssetCreated(eventId, data, event);
                    break;
                case "CustodyTransferred":
                    handleCustodyTransferred(eventId, data, event);
                    break;
                case "TestResultsSubmitted":
                    handleTestResultsSubmitted(eventId, data, event);
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
        PharmaceuticalRegistry savedBatch;

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
                savedBatch = registryRepo.save(existing);
                log.info("Successfully confirmed Batch {} on the blockchain.", data.batchNumber());

                // Trigger auto-population immediately
                populateMissingUnitsForBatch(savedBatch);
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

            // Set default batch limit to prevent nulls
            entity.setBatchUnitCount(20);

            if (event.transaction() != null) {
                entity.setBlockchainTxId(event.transaction().id());
            }

            entity.setCurrentStatus(finalStatus);
            entity.setConfirmedAt(ZonedDateTime.now().toLocalDateTime());

            savedBatch = registryRepo.save(entity);
            log.info("Successfully synchronized external Batch {} to local database.", data.batchNumber());

            // Trigger auto-population immediately
            populateMissingUnitsForBatch(savedBatch);
        }
    }

    private void handleCustodyTransferred(UUID eventId, AssetData data, FireflyEventDto event) {
        String qrHash = data.qrHash();

        SupplyChainParticipant fromParticipant = participantRepo.findById(data.fromParticipantId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + data.fromParticipantId()));

        SupplyChainParticipant toParticipant = null;
        if (data.toParticipantId() != null) {
            toParticipant = participantRepo.findById(data.toParticipantId()).orElse(null);
        }

        ChainOfCustodyEvent custodyEvent = new ChainOfCustodyEvent();
        custodyEvent.setFromParticipant(fromParticipant);
        custodyEvent.setToParticipant(toParticipant);
        custodyEvent.setEventType(data.eventType());
        custodyEvent.setQuantity(data.quantity());
        custodyEvent.setBlockchainTxId(event.transaction() != null ? event.transaction().id() : data.txId());

        Optional<SerializedUnit> unitOpt = unitRepo.findByQrHash(qrHash);

        if (unitOpt.isPresent()) {
            SerializedUnit unit = unitOpt.get();
            PharmaceuticalRegistry parentBatch = registryRepo.findById(unit.getRegistryId())
                    .orElseThrow(() -> new RuntimeException("Parent batch not found for unit"));

            custodyEvent.setRegistry(parentBatch);
            custodyEvent.setUnit(unit);

            unit.setCurrentStatus(data.eventType());
            unitRepo.save(unit);

            log.info("Recorded item-level transfer {} for Serial {}.", data.eventType(), unit.getSerialNumber());

        } else {
            PharmaceuticalRegistry registry = registryRepo.findByQrHash(qrHash)
                    .orElseThrow(() -> new RuntimeException("Asset not found for QR: " + qrHash));

            custodyEvent.setRegistry(registry);
            custodyEvent.setUnit(null);

            if ("DISPENSED".equals(data.eventType()) || "DESTROYED".equals(data.eventType())) {
                registry.setCurrentStatus(data.eventType());
                registryRepo.save(registry);
            }

            log.info("Recorded batch-level transfer {} for Batch {}.", data.eventType(), registry.getBatchNumber());
        }

        custodyRepo.save(custodyEvent);
    }

    private void handleTestResultsSubmitted(UUID eventId, AssetData data, FireflyEventDto event) {
        String qrHash = data.qrHash();

        PharmaceuticalRegistry registry = registryRepo.findByQrHash(qrHash)
                .orElseThrow(() -> new RuntimeException("Cannot log test results. Asset not found for QR: " + qrHash));

        RegulatoryScrutiny scrutiny = new RegulatoryScrutiny();
        scrutiny.setRegistry(registry);
        scrutiny.setScrutinyDate(java.time.LocalDate.now());

        try {
            scrutiny.setTestResult(com.chiyumechunga.backend.model.TestResult.valueOf(data.currentStatus()));
        } catch (IllegalArgumentException e) {
            log.warn("Could not map blockchain status '{}' to TestResult Enum.", data.currentStatus());
        }

        if (event.transaction() != null) {
            scrutiny.setBlockchainTxId(event.transaction().id());
        } else {
            scrutiny.setBlockchainTxId(data.txId() != null ? data.txId() : "UNKNOWN_TX");
        }

        scrutinyRepo.save(scrutiny);

        registry.setCurrentStatus(data.currentStatus());
        registryRepo.save(registry);

        log.info("Successfully recorded Regulatory Scrutiny for QR {}. Status: {}", qrHash, data.currentStatus());
    }

    private void saveCheckpoint(String eventId, String sequence) {
        EventCheckpoint checkpoint = new EventCheckpoint();
        checkpoint.setListenerId(eventId);
        checkpoint.setLastEventSequence(sequence);
        checkpointRepo.save(checkpoint);
    }

    // =========================================================================================
    // SYSTEM AUTOMATION: Auto-Populate Serialized Units (Primary Packaging)
    // =========================================================================================

    /**
     * Background Job: Runs every 5 minutes to find old/existing batches in the database
     * that do not have their primary serialized units generated yet.
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void backgroundSweepForMissingUnits() {
        log.debug("Running background sweep for batches missing serialized units...");

        List<PharmaceuticalRegistry> allBatches = registryRepo.findAll();
        for (PharmaceuticalRegistry batch : allBatches) {
            populateMissingUnitsForBatch(batch);
        }
    }

    /**
     * Core logic to generate child units for a parent batch.
     */
    private void populateMissingUnitsForBatch(PharmaceuticalRegistry batch) {
        List<SerializedUnit> existingUnits = unitRepo.findByRegistryId(batch.getRegistryId());

        // If the batch has no units, and its configuration requires units
        if (existingUnits.isEmpty() && batch.getBatchUnitCount() > 0) {
            log.info("SYSTEM AUTO-GEN: Creating {} serialized units for Batch {}", batch.getBatchUnitCount(), batch.getBatchNumber());

            List<SerializedUnit> newUnits = new ArrayList<>();

            for (int i = 1; i <= batch.getBatchUnitCount(); i++) {
                SerializedUnit unit = new SerializedUnit();
                unit.setRegistryId(batch.getRegistryId());

                // Generates format: BATCH-ARTN-2026-002-SN001
                String serialNumber = String.format("%s-SN%03d", batch.getBatchNumber(), i);
                unit.setSerialNumber(serialNumber);
                unit.setCurrentStatus("IN_BATCH");

                // Give each item a real, cryptographically secure hash
                unit.setQrHash(generateSHA256Hash(serialNumber));

                newUnits.add(unit);
            }

            unitRepo.saveAll(newUnits);
            log.info("Successfully populated all missing serialized units for Batch: {}", batch.getBatchNumber());
        }
    }

    /**
     * Helper method to generate realistic 64-character SHA-256 hashes for unit QR codes
     */
    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to generate secure QR hash. Falling back to UUID.", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}