package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
/*
@ExtendWith(MockitoExtension.class)
 class CustodyServiceImplTest {

    @Mock
    private FireflyIntegrationService fireflyService;

    @Mock
    private SupplyChainParticipantRepository participantRepository;

    @InjectMocks
    private CustodyServiceImpl custodyService;

    @Test
    void shouldTransferCustodySuccessfully() {
        // 1. SETUP: Create Test Data
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        String batchNumber = "BATCH-777";

        CustodyTransferRequestDto request = new CustodyTransferRequestDto(
                batchNumber, senderId, receiverId, 100, "Routine shipment"
        );

        // Mock the sender in the database (e.g., ZAMMSA)
        SupplyChainParticipant mockSender = new SupplyChainParticipant();
        mockSender.setParticipantId(senderId);
        mockSender.setRole(ParticipantType.ZAMMSA); // <--- We expect Firefly to use this role

        // 2. MOCK: Define Behavior
        Mockito.when(participantRepository.findById(senderId))
                .thenReturn(Optional.of(mockSender));

        Mockito.when(fireflyService.invokeContract(
                eq("TransferCustody"),
                any(CustodyTransferRequestDto.class),
                eq(ParticipantType.ZAMMSA) // The critical routing check
        )).thenReturn("ff-custody-operation-789");

        // 3. EXECUTE
        FireflyAckDto result = custodyService.transferCustody(request);

        // 4. VERIFY
        Assertions.assertEquals("SUBMITTED", result.status());
        Assertions.assertEquals("ff-custody-operation-789", result.operationId());

        // Verify database was checked exactly once
        Mockito.verify(participantRepository, Mockito.times(1)).findById(senderId);
    }

    @Test
    void shouldThrowExceptionWhenSenderNotFound() {
        // 1. SETUP
        UUID fakeSenderId = UUID.randomUUID();
        CustodyTransferRequestDto request = new CustodyTransferRequestDto(
                "BATCH-000", fakeSenderId, UUID.randomUUID(), 0, null
        );

        // 2. MOCK: Repository returns empty
        Mockito.when(participantRepository.findById(fakeSenderId))
                .thenReturn(Optional.empty());

        // 3. EXECUTE & VERIFY
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            custodyService.transferCustody(request);
        });

        Assertions.assertEquals("Sender not found", exception.getMessage());

        // Verify Firefly was NEVER called because the sender didn't exist
        Mockito.verify(fireflyService, Mockito.never()).invokeContract(any(), any(), any());
    }
}


 */