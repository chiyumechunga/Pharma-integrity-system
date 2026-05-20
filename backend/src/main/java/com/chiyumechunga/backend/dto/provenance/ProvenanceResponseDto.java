package com.chiyumechunga.backend.dto.provenance;

import java.time.LocalDateTime;
import java.util.List;

public record ProvenanceResponseDto(
        String verificationStatus, // e.g., "AUTHENTIC"
        LocalDateTime scanTimestamp,
        ProductDetailsDto productDetails,
        List<ProvenanceEventDto> provenanceTimeline
) {}