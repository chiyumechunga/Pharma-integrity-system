package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.EventCheckpoint;

import java.util.UUID;

public interface EventProcessingService {
    /**
     * Processes an asynchronous event received from Hyperledger Firefly.
     * Updates the local database to reflect the immutable truth on the blockchain.
     * * @param event The JSON payload from the Firefly Webhook
     */
    void processBlockchainEvent(FireflyEventDto event);

}