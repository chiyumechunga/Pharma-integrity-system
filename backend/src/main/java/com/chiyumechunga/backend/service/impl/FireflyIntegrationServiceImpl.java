package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.config.FireflyNodeRouter;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FireflyIntegrationServiceImpl implements FireflyIntegrationService {

    private final FireflyNodeRouter nodeRouter;
    private final ObjectMapper objectMapper;

    public FireflyIntegrationServiceImpl(FireflyNodeRouter nodeRouter, ObjectMapper objectMapper) {
        this.nodeRouter = nodeRouter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String invokeContract(String functionName, Object payload, ParticipantType currentUserRole) {
        log.info("Invoking '{}' as role: {}", functionName, currentUserRole);

        WebClient client = nodeRouter.getClientForRole(currentUserRole);
        Map<String, Object> inputData;

        try {
            // Align with our optimized FFI (FireFly Interface) architecture
            if ("CreateAsset".equals(functionName) || "SubmitTestResult".equals(functionName)) {
                String stringifiedJson = objectMapper.writeValueAsString(payload);
                inputData = Map.of("payloadJSON", stringifiedJson);
            } else {
                // Methods like TransferCustody expect individual mapped parameters
                inputData = objectMapper.convertValue(payload, Map.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to stringify payload for FireFly: {}", e.getMessage());
            throw new RuntimeException("JSON processing error for FireFly integration", e);
        }

        // FireFly requires payload wrapped under "input" key
        Map<String, Object> fireflyBody = Map.of("input", inputData);

        return client.post()
                .uri("/invoke/" + functionName)
                .bodyValue(fireflyBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("FireFly invoke failed: {}", e.getMessage()))
                .block();
    }
}