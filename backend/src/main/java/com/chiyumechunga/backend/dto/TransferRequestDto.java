package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferRequestDto(
        @NotBlank(message = "QR Hash is required")
        String qrHash, // Identifies the specific product box

        @NotNull(message = "From Participant ID is required")
        UUID fromParticipantId, // Who is handing it over?

        @NotNull(message = "To Participant ID is required")
        UUID toParticipantId, // Who is receiving it?

        @NotBlank(message = "Event Type is required (e.g., RECEIVED, DISTRIBUTED)")
        String eventType // Defines the nature of the transfer
) {}