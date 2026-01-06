package com.chiyumechunga.backend.dto.provenance;

import java.time.LocalDateTime;

public record ProvenanceEventDto(
        String eventType,       // e.g. "MANUFACTURED", "RECEIVED"
        String fromParticipant, // Name of sender (or "N/A")
        String toParticipant,   // Name of receiver
        LocalDateTime timestamp,
        String blockchainTxId   // The immutable proof
) {}