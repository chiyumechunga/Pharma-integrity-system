package com.chiyumechunga.backend.dto;

import java.util.UUID;

/**
 * XSS-Safe Response.
 * We only return the generated ID and a status.
 * We DO NOT echo back the user's input (Name, Description, etc.).
 */
public record ParticipantResponseDto(
        UUID participantId,      // SAFE: System-generated UUID
        String participantCode,  // SAFE: Validated strict alphanumeric code
        String status,           // SAFE: Hardcoded string
        String message           // SAFE: Hardcoded string
) {}