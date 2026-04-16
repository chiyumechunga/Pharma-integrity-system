package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.provenance.FullProvenanceDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceEventDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ChainOfCustodyEvent;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.repository.ChainOfCustodyRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.service.ProvenanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProvenanceServiceImpl implements ProvenanceService {

    private final PharmaceuticalRegistryRepository registryRepository;
    private final ChainOfCustodyRepository custodyRepository;

    public ProvenanceServiceImpl(PharmaceuticalRegistryRepository registryRepository,
                                 ChainOfCustodyRepository custodyRepository) {
        this.registryRepository = registryRepository;
        this.custodyRepository = custodyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FullProvenanceDto getProvenance(String qrHash) {
        log.info("Fetching provenance for QR: {}", qrHash);

        // 1. Get Product Details
        PharmaceuticalRegistry product = registryRepository.findByQrHash(qrHash)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with QR: " + qrHash));

        // 2. Get Event History (Ordered by Time)
        List<ChainOfCustodyEvent> events = custodyRepository.findByRegistry_QrHashOrderByEventTimestampAsc(qrHash);

        // 3. Map to DTOs
        List<ProvenanceEventDto> historyDto = events.stream().map(event -> new ProvenanceEventDto(
                event.getEventType(),
                event.getFromParticipant() != null ? event.getFromParticipant().getParticipantName() : "N/A",
                event.getToParticipant().getParticipantName(),
                event.getEventTimestamp(),
                event.getBlockchainTxId()
        )).toList();

        // 4. Derive Status (e.g., if last event was "DISPENSED", status is "SOLD")
        String currentStatus = events.isEmpty() ? "REGISTERED" : events.get(events.size() - 1).getEventType();

        return new FullProvenanceDto(
                product.getProductName(),
                product.getBatchNumber(),
                product.getManufacturer().getParticipantName(),
                product.getExpiryDate(),
                currentStatus,
                historyDto
        );
    }
}