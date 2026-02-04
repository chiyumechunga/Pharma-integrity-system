package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.config.ProfileDetails;
import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;
import com.chiyumechunga.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> loginUser(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // === ADD THIS NEW METHOD HERE ===
    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUserProfile(@AuthenticationPrincipal ProfileDetails userDetails) {
        // Direct access to the entity!
        UUID myId = userDetails.getProfile().getParticipantId();
        String myCountry = userDetails.getProfile().getCountry();
        String myRole = userDetails.getProfile().getRole().name();

        return ResponseEntity.ok("Authenticated as: " + myRole + " (ID: " + myId + ") from " + myCountry);
    }
}