package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.auth.AuthResponseDto;
import com.chiyumechunga.backend.dto.auth.ChangePasswordDto;
import com.chiyumechunga.backend.dto.auth.LoginRequestDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.PasswordResetToken;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PasswordResetTokenRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.AuthService;
import com.chiyumechunga.backend.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    // FIXED: Renamed and added missing repositories
    private final SupplyChainParticipantRepository participantRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // FIXED: Injected the tokenRepository into the constructor
    public AuthServiceImpl(SupplyChainParticipantRepository participantRepository,
                           PasswordResetTokenRepository tokenRepository,
                           JwtUtils jwtUtils) {
        this.participantRepository = participantRepository;
        this.tokenRepository = tokenRepository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        SupplyChainParticipant user = participantRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = jwtUtils.generateToken(
                user.getEmail(),
                user.getRole().toString(),
                user.getParticipantId().toString()
        );

        return new AuthResponseDto(
                token,
                "Bearer",
                user.getParticipantId().toString(),
                user.getRole().toString()
        );
    }

    @Override
    public void changePassword(UUID participantId, ChangePasswordDto request) {
        SupplyChainParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (!passwordEncoder.matches(request.oldPassword(), participant.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }

        participant.setPassword(passwordEncoder.encode(request.newPassword()));
        participantRepository.save(participant);
        log.info("Password changed successfully for participant: {}", participant.getParticipantCode());
    }

    @Override
    public void generatePasswordResetToken(String email) {
        participantRepository.findByEmail(email).ifPresent(participant -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, participant);
            tokenRepository.save(resetToken);

            log.info("\n----------------------------------------------------------");
            log.info("📧 MOCK EMAIL SENT TO: {}", email);
            log.info("To reset your password, use this token: {}", token);
            log.info("Or navigate to: http://localhost:5173/reset-password?token={}", token);
            log.info("----------------------------------------------------------\n");
        });
    }

    @Override
    public void resetPasswordWithToken(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token has expired");
        }

        SupplyChainParticipant participant = resetToken.getParticipant();
        participant.setPassword(passwordEncoder.encode(newPassword));
        participantRepository.save(participant);

        tokenRepository.delete(resetToken);
        log.info("Password successfully reset for participant via token: {}", participant.getParticipantCode());
    }
}