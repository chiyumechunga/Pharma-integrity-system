package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CustodyTransferRequestDto(

        @NotBlank(message = "Batch number is required")
        String batchNumber,

        @NotNull(message = "Sender ID is required")
        UUID fromParticipantId,

        @NotNull(message = "Receiver ID is required")
        UUID toParticipantId,

        // Optional: Useful if you want to track specific quantities (e.g., partial shipments)
        Integer quantity,

        // Optional: Notes for the blockchain record
        String notes
) {}