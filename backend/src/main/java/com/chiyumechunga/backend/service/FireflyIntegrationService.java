package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.RegistryRequestDto;

public interface FireflyIntegrationService {
    /**
     * Sends a contract invocation to Hyperledger Firefly.
     * @param method The name of the function in Chaincode (e.g., "CreateAsset")
     * @param request The data payload
     * @return The Operation ID from Firefly
     */
    String invokeContract(String method, RegistryRequestDto request);
}