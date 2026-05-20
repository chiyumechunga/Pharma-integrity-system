package com.chiyumechunga.backend.dto.auth;

public record ResetPasswordDto(String token, String newPassword) {}