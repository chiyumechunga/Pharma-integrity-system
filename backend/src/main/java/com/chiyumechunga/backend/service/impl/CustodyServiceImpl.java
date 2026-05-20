package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.config.ProfileDetails;
import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ChainOfCustodyEvent;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SerializedUnit;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.ChainOfCustodyRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SerializedUnitRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.CustodyService;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustodyServiceImpl implements CustodyService {

    private final FireflyIntegrationService fireflyService;
    private final SupplyChainParticipantRepository participantRepository;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final SerializedUnitRepository serializedUnitRepository;
    private final ChainOfCustodyRepository custodyRepository;

    @Override
    public FireflyAckDto transferCustody(CustodyTransferRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ProfileDetails userDetails = (ProfileDetails) authentication.getPrincipal();
        UUID senderId = userDetails.getProfile().getParticipantId();

        if (!senderId.equals(request.fromParticipantId())) {
            throw new AccessDeniedException("Unauthorized: You cannot initiate a transfer on behalf of another participant.");
        }

        SupplyChainParticipant sender = participantRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found or unauthorized"));

        // Only enforce batch ownership check if a batch number was provided
        if (request.batchNumber() != null && !request.batchNumber().isBlank()) {
            boolean ownsBatch = registryRepository.isBatchOwnedBy(request.batchNumber(), senderId);
            if (!ownsBatch) {
                throw new AccessDeniedException("You do not currently hold custody of batch: " + request.batchNumber());
            }
        }

        String operationId = fireflyService.invokeContract(
                "TransferCustody",
                request,
                sender.getRole()
        );

        return new FireflyAckDto(operationId, "SUBMITTED", "Custody transfer initiated on Blockchain.");
    }

    @Override
    @Transactional
    public FireflyAckDto dispenseUnit(String serialNumber, UUID pharmacyId) {
        // 1. Fetch the specific bottle
        SerializedUnit unit = serializedUnitRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found"));

        if ("DISPENSED".equals(unit.getCurrentStatus())) {
            throw new IllegalArgumentException("Unit is already dispensed.");
        }
        if ("RECALLED".equals(unit.getCurrentStatus())) {
            throw new IllegalArgumentException("Cannot dispense a recalled unit.");
        }

        // 2. Fetch the Pharmacy
        SupplyChainParticipant pharmacy = participantRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found"));

        // 3. Update the item's status logically
        unit.setCurrentStatus("DISPENSED");
        serializedUnitRepository.save(unit);

        // 4. Create the Immutable Ledger Event in Postgres
        ChainOfCustodyEvent event = new ChainOfCustodyEvent();
        PharmaceuticalRegistry batch = registryRepository.findById(unit.getRegistryId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent batch missing"));

        event.setRegistry(batch);
        event.setUnit(unit);
        event.setFromParticipant(pharmacy);
        event.setToParticipant(null); // Null implies End Consumer / Patient
        event.setEventType("DISPENSED");
        event.setQuantity(1);
        event.setBlockchainTxId("pending-" + UUID.randomUUID().toString().replace("-", ""));
        custodyRepository.save(event);

        // 5. Build Blockchain Payload mapped EXACTLY to your ffi.json
        Map<String, Object> payload = new HashMap<>();
        payload.put("qrHash", unit.getQrHash()); // The blockchain indexes by this hash!
        payload.put("fromId", pharmacy.getParticipantId().toString());
        payload.put("toId", ""); // Patient doesn't have an ID
        payload.put("eventType", "DISPENSED");
        payload.put("quantity", 1);

        // Call the EXISTING smart contract method
        String rawResponse = fireflyService.invokeContract(
                "TransferCustody",
                payload,
                ParticipantType.PHARMACY
        );

        return new FireflyAckDto("Success", "DISPENSED", "Unit successfully dispensed to patient.");
    }
}