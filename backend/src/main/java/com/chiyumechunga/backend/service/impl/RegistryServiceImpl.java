package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.model.ParticipantType; // <--- Import this
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private final FireflyIntegrationService fireflyService;

    public RegistryServiceImpl(FireflyIntegrationService fireflyService) {
        this.fireflyService = fireflyService;
    }

    @Override
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("Received batch request for Product: {}. Routing to Firefly...", request.productName());

        // FIX: Pass 'ParticipantType.MANUFACTURER' as the 3rd argument.
        // This tells the router to use Port 7000 (Manufacturer Node).
        String operationId = fireflyService.invokeContract(
                "CreateAsset",
                request,
                ParticipantType.MANUFACTURER
        );

        return new FireflyAckDto(operationId, "SUBMITTED", "Batch registration initiated successfully.");
    }
}