package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.AuthService;
import com.chiyumechunga.backend.util.JwtUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Import needed for password check
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SupplyChainParticipantRepository repository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Initialize encoder

    public AuthServiceImpl(SupplyChainParticipantRepository repository, JwtUtils jwtUtils) {
        this.repository = repository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        // 1. Find User by Email
        SupplyChainParticipant user = repository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        // 2. VERIFY PASSWORD (CRITICAL FIX: You were missing this check!)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials"); // In production use specific exception
        }

        // 3. Generate Token
        String token = jwtUtils.generateToken(
                user.getEmail(),
                user.getRole().toString(),
                user.getParticipantId().toString()
        );

        // 4. Return Response
        return new AuthResponseDto(
                token,
                "Bearer",
                user.getParticipantId().toString(),
                user.getRole().toString()
        );
    }
}