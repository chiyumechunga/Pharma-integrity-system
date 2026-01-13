package com.chiyumechunga.backend.dto;

import java.util.UUID;

public record ParticipantProfileDto(
        UUID participantId,
        String participantName,
        String participantCode,
        String country,
        String role,
        String status, // "ACTIVE", "SUSPENDED"
        String blockchainEnrollmentId
) {}