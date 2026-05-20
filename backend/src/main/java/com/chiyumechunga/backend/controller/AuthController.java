package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.config.ProfileDetails;
import com.chiyumechunga.backend.dto.auth.*;
import com.chiyumechunga.backend.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
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
        // MODERN APPROACH: Use ProblemDetail (RFC 7807) instead of raw Maps for errors
        if (authentication == null || userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Valid session required to access profile."));
        }

        // Java 10+ 'var' and Java 16+ '.toList()'
        var authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var profile = userDetails.getProfile();

        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "authorities", authorities,
                "participantId", profile.getParticipantId(),
                "country", profile.getCountry(),
                "role", profile.getRole().name()
        ));
    }

    // --- CHANGE PASSWORD (Authenticated) ---
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal ProfileDetails userDetails,
            @RequestBody ChangePasswordDto request) {

        // Ideally, Spring Security blocks unauthorized users before this line.
        // But if checking is a must, ProblemDetail is used.
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Must be logged in to change password."));
        }

        authService.changePassword(userDetails.getProfile().getParticipantId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // --- FORGOT PASSWORD (Public) ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ForgotPasswordRequestDto request) {
        log.info("Password reset requested for email: {}", request.email());

        // Anti-enumeration design remains excellent here.
        authService.generatePasswordResetToken(request.email());

        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
    }

    // --- RESET PASSWORD (Public, requires Token) ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request) {
        authService.resetPasswordWithToken(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now login."));
    }
}