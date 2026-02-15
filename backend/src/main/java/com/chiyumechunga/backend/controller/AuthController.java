package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.config.ProfileDetails;
import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;
import com.chiyumechunga.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }



    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Not Authenticated");
        }

        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                // This is the CRITICAL part. It lists exactly what permissions you have.
                // If this list is empty, JwtAuthenticationFilter is broken.
                // If it says "ROLE_MANUFACTURER", you need hasRole().
                // If it says "MANUFACTURER", you need hasAuthority().
                "authorities", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        ));
    }
    /* === ADD THIS NEW METHOD HERE ===
    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUserProfile(@AuthenticationPrincipal ProfileDetails userDetails) {
        // Direct access to the entity!
        UUID myId = userDetails.getProfile().getParticipantId();
        String myCountry = userDetails.getProfile().getCountry();
        String myRole = userDetails.getProfile().getRole().name();

        return ResponseEntity.ok("Authenticated as: " + myRole + " (ID: " + myId + ") from " + myCountry);
    }*/
}