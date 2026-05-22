package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.provenance.ProductDetailsDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceEventDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceResponseDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductVerification;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.repository.*;
import com.chiyumechunga.backend.service.ProvenanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ProvenanceServiceImpl implements ProvenanceService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final SerializedUnitRepository serializedUnitRepository;
    private final ChainOfCustodyRepository custodyRepository;
    private final ProductVerificationRepository verificationRepository;
    private final SupplyChainParticipantRepository participantRepository;

    public ProvenanceServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                 SerializedUnitRepository serializedUnitRepository,
                                 ChainOfCustodyRepository custodyRepository,
                                 ProductVerificationRepository verificationRepository,
                                 SupplyChainParticipantRepository participantRepository) {
        this.registryRepository = registryRepository;
        this.serializedUnitRepository = serializedUnitRepository;
        this.custodyRepository = custodyRepository;
        this.verificationRepository = verificationRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProvenanceResponseDto getProvenance(String qrHash) {
        log.info("Fetching anchored off-chain provenance for QR: {}", qrHash);

        // 1. ITEM-LEVEL SCAN
        Optional<SerializedUnit> unitOpt = serializedUnitRepository.findByQrHash(qrHash);
        if (unitOpt.isPresent()) {
            SerializedUnit unit = unitOpt.get();
            PharmaceuticalRegistry parentBatch = registryRepository.findById(unit.getRegistryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent batch missing for this unit"));

            return buildResponse(parentBatch, qrHash, false, unit.getSerialNumber(), unit.getCurrentStatus());
        }

        // 2. BATCH-LEVEL SCAN
        Optional<PharmaceuticalRegistry> batchOpt = registryRepository.findByQrHash(qrHash);
        if (batchOpt.isPresent()) {
            PharmaceuticalRegistry batch = batchOpt.get();
            return buildResponse(batch, qrHash, true, null, batch.getCurrentStatus());
        }

        throw new ResourceNotFoundException("No product found for the provided QR hash");
    }

    private ProvenanceResponseDto buildResponse(PharmaceuticalRegistry registry, String requestedQrHash, boolean isBatch, String serialNumber, String systemStatus) {
        ProductVerification latestScan = verificationRepository
                .findTopByPharmaceuticalRegistry_RegistryIdOrderByScanTimestampDesc(registry.getRegistryId())
                .orElse(null);

        // Autonomous Status Calculation
        String status = "AUTHENTIC";
        if ("RECALLED".equals(systemStatus) || "COUNTERFEIT".equals(systemStatus)) {
            status = systemStatus;
        } else if (registry.getExpiryDate() != null && registry.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            status = "EXPIRED";
        }

        if (latestScan != null && ("COUNTERFEIT".equals(latestScan.getVerificationStatus()) || "RECALLED".equals(latestScan.getVerificationStatus()))) {
            status = latestScan.getVerificationStatus();
        }

        LocalDateTime scanTime = latestScan != null ? latestScan.getScanTimestamp() : LocalDateTime.now();
        String manufacturerName = registry.getManufacturer() != null ? registry.getManufacturer().getParticipantName() : "Unknown Manufacturer";

        ProductDetailsDto productDetails = new ProductDetailsDto(
                registry.getProductName(),
                registry.getBatchNumber(),
                manufacturerName,
                serialNumber,
                registry.getExpiryDate(),
                systemStatus
        );

        List<ProvenanceEventDto> finalTimeline = new ArrayList<>();

        if (isBatch) {
            finalTimeline.addAll(fetchOffChainTimeline(requestedQrHash));
        } else {
            finalTimeline.addAll(fetchOffChainTimeline(registry.getQrHash()));
            finalTimeline.addAll(fetchOffChainTimeline(requestedQrHash));
        }

        List<ProvenanceEventDto> sortedTimeline = finalTimeline.stream()
                .distinct()
                .sorted(Comparator.comparing(ProvenanceEventDto::eventTimestamp).reversed())
                .toList();

        return new ProvenanceResponseDto(status, scanTime, productDetails, sortedTimeline);
    }

    private List<ProvenanceEventDto> fetchOffChainTimeline(String qrHash) {
        return custodyRepository.getProductProvenance(qrHash)
                .stream()
                .map(row -> {
                    // Resolve actual participant names from the database using the stored UUIDs
                    String fromName = resolveParticipantName(row.getFromParticipantName());
                    String toName = resolveParticipantName(row.getToParticipantName());

                    return new ProvenanceEventDto(
                            row.getEventTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            row.getEventType(),
                            row.getFromParticipantName(), // Passed as hash fallback
                            fromName,                     // Actual Human Name
                            row.getToParticipantName(),   // Passed as hash fallback
                            toName,                       // Actual Human Name
                            row.getBlockchainTxId()
                    );
                }).toList();
    }

    private String resolveParticipantName(String identifier) {
        if (identifier == null || identifier.isBlank()) return "System";
        try {
            return participantRepository.findById(UUID.fromString(identifier))
                    .map(p -> p.getParticipantName())
                    .orElse("Unknown Entity");
        } catch (IllegalArgumentException e) {
            return identifier; // Return raw string if not a valid UUID
        }
    }
}