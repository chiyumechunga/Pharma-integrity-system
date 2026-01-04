package com.chiyumechunga.backend.dto;

/**
 * DTO for the immediate acknowledgment of an asynchronous blockchain submission.
 * Since Firefly processes transactions asynchronously, we return an 'Operation ID'
 * so the client can track the status later.
 */
public record FireflyAckDto(
        String operationId, // The ID returned by Firefly to track this specific operation
        String status,      // e.g., "SUBMITTED", "PENDING"
        // e.g., "Request submitted to Blockchain. Awaiting confirmation."
        String s) {}