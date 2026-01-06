package com.chiyumechunga.backend.mytests;

// Imports are now required because the test is in a different package
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.impl.RegistryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RegistryServiceImplTest {

    @Mock
    private FireflyIntegrationService fireflyService;

    @InjectMocks
    private RegistryServiceImpl registryService;

    @Test
    void shouldRegisterBatchSuccessfully() {
        // 1. SETUP: Create fake data
        UUID mfgId = UUID.randomUUID();
        RegistryRequestDto request = new RegistryRequestDto(
                "Panadol",
                "BATCH-001",
                mfgId,
                LocalDate.now().plusYears(1),
                "abc123hash"
        );

        // 2. MOCK: Simulate Firefly response
        Mockito.when(fireflyService.invokeContract(Mockito.eq("CreateAsset"), Mockito.any()))
                .thenReturn("ff-operation-123");

        // 3. EXECUTE
        FireflyAckDto result = registryService.registerBatch(request);

        // 4. VERIFY
        Assertions.assertEquals("SUBMITTED", result.status());
        Assertions.assertEquals("ff-operation-123", result.operationId());

        Mockito.verify(fireflyService, Mockito.times(1))
                .invokeContract(Mockito.eq("CreateAsset"), Mockito.eq(request));
    }
}