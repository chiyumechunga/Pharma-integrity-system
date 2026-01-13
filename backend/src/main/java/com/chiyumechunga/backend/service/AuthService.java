package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto request);
}