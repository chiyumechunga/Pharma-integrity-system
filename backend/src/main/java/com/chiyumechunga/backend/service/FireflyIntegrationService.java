package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.model.ParticipantType;

public interface FireflyIntegrationService {
    /**
     * Invokes a smart contract function on the blockchain.
     * @param functionName The name of the function (e.g., "CreateAsset")
     * @param payload The DTO containing the data
     * @param role The role of the user invoking it (Determines which Node is used)
     * @return The Operation ID from Firefly
     */
    String invokeContract(String functionName, Object payload, ParticipantType role);
}