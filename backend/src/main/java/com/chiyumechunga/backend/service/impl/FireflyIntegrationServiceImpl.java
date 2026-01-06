package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.firefly.FireflyContractInvokeDto;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FireflyIntegrationServiceImpl implements FireflyIntegrationService {

    private final WebClient fireflyWebClient;
    private final ObjectMapper objectMapper; // Helper to convert DTOs to Map

    public FireflyIntegrationServiceImpl(WebClient fireflyWebClient, ObjectMapper objectMapper) {
        this.fireflyWebClient = fireflyWebClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String invokeContract(String functionName, Object payload) { // <--- Changed to Object
        log.info("Invoking Contract Function: {}", functionName);

        // 1. Convert any DTO to a Map (JSON structure)
        // This works for RegistryRequestDto, LabInspectionRequestDto, etc.
        Map<String, Object> inputData = objectMapper.convertValue(payload, Map.class);

        // 2. Build the Firefly Payload
        FireflyContractInvokeDto requestBody = new FireflyContractInvokeDto(
                new FireflyContractInvokeDto.Location("pharma-integrity-contract"),
                new FireflyContractInvokeDto.Method(functionName),
                inputData
        );

        // 3. Send to Firefly Node
        // (Assuming synchronous for simplicity, though async is usually better for blockchain)
        return fireflyWebClient.post()
                .uri("/contracts/invoke")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class) // Returns the Operation ID
                .block();
    }
}