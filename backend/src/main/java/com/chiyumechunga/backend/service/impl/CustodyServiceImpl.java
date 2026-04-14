package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.CustodyService;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustodyServiceImpl implements CustodyService {

    private final FireflyIntegrationService fireflyService;
    private final SupplyChainParticipantRepository participantRepository;

    private final PharmaceuticalRegistryRepository registryRepository;

    @Override
    public FireflyAckDto transferCustody(CustodyTransferRequestDto request) {
        String authenticatedUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID senderId = UUID.fromString(authenticatedUserId);

        SupplyChainParticipant sender = participantRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found or unauthorized"));

        // Calls the method on the injected instance (registryRepository), NOT the Class name
        boolean ownsBatch = registryRepository.isBatchOwnedBy(request.batchNumber(), senderId);
        if (!ownsBatch) {
            throw new AccessDeniedException("You do not currently hold custody of batch: " + request.batchNumber());
        }


        ParticipantType senderRole = sender.getRole();

        String operationId = fireflyService.invokeContract(
                "TransferCustody",
                request,
                senderRole
        );

        return new FireflyAckDto(operationId, "SUBMITTED", "Custody transfer initiated on Blockchain.");
    }
}