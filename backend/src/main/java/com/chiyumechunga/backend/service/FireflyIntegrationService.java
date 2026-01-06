package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.RegistryRequestDto;

public interface FireflyIntegrationService {
    /**
     * Invokes a smart contract function.
     * @param functionName The name of the function in your Chaincode (e.g., "CreateAsset").
     * @param payload The data DTO (RegistryRequestDto, LabInspectionRequestDto, etc.).
     */
    String invokeContract(String functionName, Object payload); // <--- Changed to Object
}