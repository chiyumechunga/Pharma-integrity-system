package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.config.FireflyNodeRouter;
import com.chiyumechunga.backend.dto.CustodyTransferRequestDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FireflyIntegrationServiceImpl implements FireflyIntegrationService {

    private final FireflyNodeRouter nodeRouter;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String fireflyBaseUrl;

    public FireflyIntegrationServiceImpl(
            FireflyNodeRouter nodeRouter,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            @Value("""
                    ${firefly.api.url:http://localhost:5000}""") String fireflyBaseUrl) {
        this.nodeRouter = nodeRouter;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.fireflyBaseUrl = fireflyBaseUrl;
    }

    @Override
    public String invokeContract(String functionName, Object payload, ParticipantType currentUserRole) {
        log.info("Invoking '{}' as role: {}", functionName, currentUserRole);

        WebClient client = nodeRouter.getClientForRole(currentUserRole);
        Map<String, Object> inputData;

        try {
            if ("CreateAsset".equals(functionName) || "SubmitTestResult".equals(functionName)) {
                String stringifiedJson = objectMapper.writeValueAsString(payload);
                inputData = Map.of("payloadJSON", stringifiedJson);

            } else if ("TransferCustody".equals(functionName)) {
                CustodyTransferRequestDto dto = (CustodyTransferRequestDto) payload;

                inputData = Map.of(
                        "qrHash", dto.batchNumber(),
                        "fromId", dto.fromParticipantId().toString(),
                        "toId", dto.toParticipantId().toString(),
                        "eventType", "TRANSFER",
                        "quantity", dto.quantity() != null ? dto.quantity() : 1
                );
            } else {
                inputData = objectMapper.convertValue(payload, Map.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to stringify payload for FireFly: {}", e.getMessage());
            throw new RuntimeException("JSON processing error for FireFly integration", e);
        }

        Map<String, Object> fireflyBody = Map.of("input", inputData);

        return client.post()
                .uri("/invoke/" + functionName)
                .bodyValue(fireflyBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("FireFly invoke failed: {}", e.getMessage()))
                .block();
    }

    @Override
    public Object getOperationStatus(String operationId) {
        String url = String.format("%s/api/v1/namespaces/default/operations/%s", fireflyBaseUrl, operationId);
        log.debug("Calling Firefly API: GET {}", url);

        return restTemplate.getForObject(url, Object.class);
    }

    @Override
    public String queryContract(String functionName, Object payload, ParticipantType currentUserRole) {
        log.info("Querying contract '{}' as role: {}", functionName, currentUserRole);

        WebClient client = nodeRouter.getClientForRole(currentUserRole);

        // Assuming your smart contract query expects the qrHash as an argument
        Map<String, Object> inputData = Map.of("qrHash", payload.toString());
        Map<String, Object> fireflyBody = Map.of("input", inputData);

        return client.post()
                .uri("/query/" + functionName) // Using /query/ instead of /invoke/
                .bodyValue(fireflyBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("FireFly query failed: {}", e.getMessage()))
                .block();
    }
}