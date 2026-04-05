package com.chiyumechunga.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponseDto(
        UUID productId,
        String productCode,
        String genericName,
        String brandName,
        String dosageForm,
        String strength,
        String therapeuticClass,
        boolean requiresColdChain,
        boolean approvedByZamra,
        LocalDateTime createdAt
) {}