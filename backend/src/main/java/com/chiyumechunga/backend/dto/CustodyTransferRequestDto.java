package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CustodyTransferRequestDto(

        String batchNumber,

        // Used for item-level transit (e.g., Pharmacy -> Patient)
        String serialNumber,

        @NotNull(message = "Sender ID is required")
        UUID fromParticipantId,

        @NotNull(message = "Receiver ID is required")
        UUID toParticipantId,

        // Optional: Useful if you want to track specific quantities (e.g., partial shipments)
        Integer quantity,

        // Optional: Notes for the blockchain record
        String notes
) {
        /**
         * Custom Spring Validation Hook.
         * Evaluates to true only if one identifier is present and the other is null/blank.
         */
        @AssertTrue(message = "Request must contain exactly one identifier: batchNumber OR serialNumber")
        public boolean isValidIdentifier() {
                boolean hasBatch = batchNumber != null && !batchNumber.isBlank();
                boolean hasSerial = serialNumber != null && !serialNumber.isBlank();

                // XOR logic: True if exactly one is true, false if both are true or both are false
                return hasBatch ^ hasSerial;
        }
}