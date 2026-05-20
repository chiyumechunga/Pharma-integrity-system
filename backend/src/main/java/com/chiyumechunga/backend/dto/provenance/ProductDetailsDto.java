package com.chiyumechunga.backend.dto.provenance;

import java.time.LocalDate;

public record ProductDetailsDto(
        String genericName,
        String batchNumber,
        String manufacturer,
        String serialNumber,
        LocalDate expiryDate,
        String currentStatus
) {}