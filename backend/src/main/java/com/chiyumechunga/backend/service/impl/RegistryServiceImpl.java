package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
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

    /**
     * COMMAND SIDE:
     * Accepts the payload and immediately pushes it to Firefly.
     * Does NOT write to the local database yet.
     */
    @Override
    public FireflyAckDto registerBatch(RegistryRequestDto request) {
        log.info("Received batch request for Product: {}. Routing to Firefly...", request.productName());

        // 1. Construct the payload for the Smart Contract
        // In Firefly, we typically invoke a contract interface or broadcast a message
        // This corresponds to the "API Request -> FireFly REST" step
        String operationId = fireflyService.invokeContract("CreateAsset", request);

        // 2. Return an acknowledgment (HTTP 202 Accepted semantics)
        // We tell the user "We received it, check back later"
        return new FireflyAckDto(operationId, "Request submitted to Blockchain. Awaiting confirmation.", "Batch registration initiated successfully.");
    }
}