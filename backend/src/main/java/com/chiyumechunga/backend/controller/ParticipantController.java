package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.ParticipantProfileDto; // The New DTO
import com.chiyumechunga.backend.dto.ParticipantRegistrationDto;
import com.chiyumechunga.backend.dto.ParticipantResponseDto;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.service.ParticipantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    // --- 1. REGISTRATION (Fixed: Handles Password) ---
    @PostMapping
    public ResponseEntity<ParticipantResponseDto> register(@Valid @RequestBody ParticipantRegistrationDto request) {
        log.info("Request to onboard participant: {}", request.participantCode());

        // Sanitization (Password is NOT sanitized here, it's hashed in Service)
        ParticipantRegistrationDto safeRequest = new ParticipantRegistrationDto(
                sanitizeStrict(request.participantCode()),
                HtmlUtils.htmlEscape(request.participantName()),
                request.participantType(),
                HtmlUtils.htmlEscape(request.country()),
                sanitizeStrict(request.blockchainEnrollmentId()),
                request.password() // Pass raw password to Service for BCrypt hashing
        );

        SupplyChainParticipant newParticipant = participantService.registerParticipant(safeRequest);

        // Response DTO (Strictly for "Registration Success" message)
        ParticipantResponseDto cleanResponse = new ParticipantResponseDto(
                newParticipant.getParticipantId(),
                newParticipant.getParticipantCode(),
                "CREATED",
                "Participant registered successfully. Please login."
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(cleanResponse, headers, HttpStatus.CREATED);
    }

    // --- 2. GET PROFILE (Fixed: Returns Safe DTO, No Password Leaks) ---
    @GetMapping("/{id}")
    public ResponseEntity<ParticipantProfileDto> getParticipant(@PathVariable UUID id) {
        SupplyChainParticipant p = participantService.getParticipantById(id);

        // Map Entity -> Safe DTO
        return ResponseEntity.ok(new ParticipantProfileDto(
                p.getParticipantId(),
                p.getParticipantName(),
                p.getParticipantCode(),
                p.getCountry(),
                p.getRole().toString(),
                p.isActive() ? "ACTIVE" : "SUSPENDED", // Assuming you have an 'isActive' boolean
                p.getBlockchainEnrollmentId()
        ));
    }

    // --- 3. LIST ALL (Fixed: Used for Dashboard Table) ---
    @GetMapping
    public ResponseEntity<List<ParticipantProfileDto>> getAllParticipants() {
        List<SupplyChainParticipant> participants = participantService.getAllParticipants();

        // Map List -> List<Safe DTO>
        List<ParticipantProfileDto> response = participants.stream()
                .map(p -> new ParticipantProfileDto(
                        p.getParticipantId(),
                        p.getParticipantName(),
                        p.getParticipantCode(),
                        p.getCountry(),
                        p.getRole().toString(),
                        p.isActive() ? "ACTIVE" : "SUSPENDED",
                        p.getBlockchainEnrollmentId()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // --- 4. APPROVE/SUSPEND (Regulatory Logic) ---
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestParam boolean active) {
        log.info("Admin updating status for participant {} to Active={}", id, active);
        participantService.updateParticipantStatus(id, active);
        return ResponseEntity.ok().build();
    }

    private String sanitizeStrict(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}