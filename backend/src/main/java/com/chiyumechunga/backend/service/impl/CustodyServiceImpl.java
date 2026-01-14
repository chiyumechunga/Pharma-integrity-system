package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.CustodyService;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustodyServiceImpl implements CustodyService {

    private final FireflyIntegrationService fireflyService;
    private final SupplyChainParticipantRepository participantRepository;

    @Override
    public FireflyAckDto transferCustody(CustodyTransferRequestDto request) {
        log.info("Initiating Custody Transfer: Batch {} -> Participant {}", request.batchNumber(), request.toParticipantId());

        // 1. IDENTIFY THE SENDER (Who is logged in?)
        // In a real app, you get this from the SecurityContext (JWT).
        // For this demo, we can assume the 'request' carries the sender's ID or look it up.
        SupplyChainParticipant sender = participantRepository.findById(request.fromParticipantId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        // 2. DETERMINE ROLE (The Critical Fix)
        // We must tell Firefly WHICH node to use based on who is sending the drug.
        ParticipantType senderRole = sender.getRole(); // e.g., ZAMMSA

        // 3. INVOKE SMART CONTRACT
        // Now calling with the 3rd argument (senderRole) fixes the compilation error.
        String operationId = fireflyService.invokeContract(
                "TransferCustody",
                request,
                senderRole // <--- Routes to Port 7001 (ZAMMSA) or 7002 (Pharmacy) automatically
        );

        return new FireflyAckDto(operationId, "SUBMITTED", "Custody transfer initiated on Blockchain.");
    }
}