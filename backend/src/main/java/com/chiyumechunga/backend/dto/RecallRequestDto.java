package com.chiyumechunga.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecallRequestDto(
        @NotBlank(message = "Batch number is required")
        String batchNumber,

        @NotBlank(message = "Severity level is required")
        String severityLevel,

        @NotBlank(message = "Recall reason is required")
        String recallReason,

        @NotNull(message = "Initiator ID is required")
        UUID initiatedBy
) {}