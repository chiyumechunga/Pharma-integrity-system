package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.ChangePasswordDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;

import java.util.UUID;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto request);

    void changePassword(UUID participantId, ChangePasswordDto request);
    void generatePasswordResetToken(String email);
    void resetPasswordWithToken(String token, String newPassword);
}