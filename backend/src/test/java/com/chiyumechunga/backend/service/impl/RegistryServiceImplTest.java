package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RegistryServiceImplTest {

    @Mock
    private FireflyIntegrationService fireflyService;

    @InjectMocks
    private RegistryServiceImpl registryService;

    @Test
    void shouldRegisterBatchSuccessfully() {
        // 1. SETUP: Create valid test data
        UUID mfgId = UUID.randomUUID();
        RegistryRequestDto request = new RegistryRequestDto(
                "Panadol",
                "BATCH-001",
                mfgId,
                LocalDate.now().plusYears(1),
                "abc123hash"
        );

        // 2. MOCK: Simulate the Firefly Service response
        // CRITICAL: We must match the 3 arguments (String, Object, Role)
        Mockito.when(fireflyService.invokeContract(
                eq("CreateAsset"),           // 1. Function Name
                any(RegistryRequestDto.class), // 2. Payload
                eq(ParticipantType.MANUFACTURER) // 3. Role (The routing fix)
        )).thenReturn("ff-operation-123");

        // 3. EXECUTE: Call the actual service method
        FireflyAckDto result = registryService.registerBatch(request);

        // 4. VERIFY: Check the results
        Assertions.assertEquals("SUBMITTED", result.status());
        Assertions.assertEquals("ff-operation-123", result.operationId());

        // Verify the mock was called exactly once with the correct parameters
        Mockito.verify(fireflyService, Mockito.times(1))
                .invokeContract(
                        eq("CreateAsset"),
                        eq(request),
                        eq(ParticipantType.MANUFACTURER)
                );
    }
}