package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.config.FireflyNodeRouter;
import com.chiyumechunga.backend.dto.firefly.FireflyContractInvokeDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class FireflyIntegrationServiceImpl implements FireflyIntegrationService {

    // REPLACED: private final WebClient fireflyWebClient;
    private final FireflyNodeRouter nodeRouter; // <--- The new dynamic router
    private final ObjectMapper objectMapper;

    public FireflyIntegrationServiceImpl(FireflyNodeRouter nodeRouter, ObjectMapper objectMapper) {
        this.nodeRouter = nodeRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * Now accepts 'currentUserRole' to determine WHICH node to talk to.
     */
    @Override
    public String invokeContract(String functionName, Object payload, ParticipantType currentUserRole) {
        log.info("Invoking Contract Function: '{}' as Role: {}", functionName, currentUserRole);

        // 1. SELECT THE CORRECT NODE
        // If role is MANUFACTURER -> Uses http://localhost:7000
        // If role is ZAMMSA       -> Uses http://localhost:7001
        WebClient client = nodeRouter.getClientForRole(currentUserRole);

        // 2. PREPARE DATA
        Map<String, Object> inputData = objectMapper.convertValue(payload, Map.class);

        FireflyContractInvokeDto requestBody = new FireflyContractInvokeDto(
                new FireflyContractInvokeDto.Location("pharma-integrity-contract"),
                new FireflyContractInvokeDto.Method(functionName),
                inputData
        );

        // 3. SEND REQUEST (To the specific node selected above)
        return client.post()
                .uri("/contracts/invoke")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}