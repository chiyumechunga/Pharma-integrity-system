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
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication,
            @AuthenticationPrincipal ProfileDetails userDetails
    ) {
        if (authentication == null || userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Not Authenticated"
            ));
        }

        String username = authentication.getName();
        var authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        UUID participantId = userDetails.getProfile().getParticipantId();
        String country = userDetails.getProfile().getCountry();
        String role = userDetails.getProfile().getRole().name();

        return ResponseEntity.ok(Map.of(
                "username", username,
                "authorities", authorities,
                "participantId", participantId,
                "country", country,
                "role", role
        ));
    }

}