package com.chiyumechunga.backend.dto.provenance;

import java.time.LocalDate;
import java.util.List;

public record FullProvenanceDto(
        String productName,
        String batchNumber,
        String manufacturerName,
        LocalDate expiryDate,
        String currentStatus,   // Derived from the latest event
        List<ProvenanceEventDto> history // The timeline
) {}