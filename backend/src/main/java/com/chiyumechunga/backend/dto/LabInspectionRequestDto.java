package com.chiyumechunga.backend.dto;

import com.chiyumechunga.backend.model.TestResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LabInspectionRequestDto(
        @NotNull(message = "Product (Registry ID) is required")
        UUID registryId,

        @NotNull(message = "Inspector ID is required")
        UUID inspectorId,

        @NotNull(message = "Test Result is required")
        TestResult testResult,

        @NotBlank(message = "Lab Notes are required")
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String labNotes
) {}