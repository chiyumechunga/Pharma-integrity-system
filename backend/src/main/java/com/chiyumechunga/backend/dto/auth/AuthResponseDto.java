package com.chiyumechunga.backend.dto.auth;

public record AuthResponseDto(
        String token,
        String type,        // "Bearer"
        String userId,
        String role         // "MANUFACTURER", "INSPECTOR", "PHARMACY"
) {}