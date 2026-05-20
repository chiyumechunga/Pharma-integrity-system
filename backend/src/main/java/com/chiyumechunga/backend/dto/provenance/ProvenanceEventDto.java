package com.chiyumechunga.backend.dto.provenance;

import java.time.LocalDateTime;

public record ProvenanceEventDto(
        LocalDateTime eventTimestamp,
        String eventType,
        String fromParticipant,       // The raw hash
        String fromParticipantName,   // NEW: The human-readable name
        String toParticipant,         // The raw hash
        String toParticipantName, // NEW: The human-readable
        String blockchainTxId ) {}

