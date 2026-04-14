package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.ParticipantRegistrationDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.ParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Ensure you have this dependency
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ParticipantServiceImpl implements ParticipantService {

    private final SupplyChainParticipantRepository repository;
    private final BCryptPasswordEncoder passwordEncoder; // To hash passwords

    public ParticipantServiceImpl(SupplyChainParticipantRepository repository) {
        this.repository = repository;
        this.passwordEncoder = new BCryptPasswordEncoder(); // Or inject via Bean
    }

    @Override
    public SupplyChainParticipant registerParticipant(ParticipantRegistrationDto request) {
        // 1. Check for duplicates
        if (repository.existsByParticipantCode(request.participantCode())) {
            throw new DuplicateResourceException("Participant Code already exists: " + request.participantCode());
        }
        if (repository.findByEmail(request.participantCode()).isPresent()) {
            // Note: Ideally DTO should have 'email', treating 'code' as unique ID for now
        }

        // 2. Map DTO to Entity
        SupplyChainParticipant participant = new SupplyChainParticipant();
        participant.setParticipantCode(request.participantCode());
        participant.setParticipantName(request.participantName());
        participant.setCountry(request.country());
        participant.setBlockchainEnrollmentId(request.blockchainEnrollmentId());
        participant.setEmail(request.email());

        // 3. Set Defaults & Security
        // CORRECT: Use the existing field name from your DTO
        participant.setRole(request.participantType());
        participant.setActive(true); // Default to active? Or false if approval needed?

        // CRITICAL: Hash the password before saving!
        if (request.password() != null) {
            participant.setPassword(passwordEncoder.encode(request.password()));
        }

        // 4. Save
        log.info("Registering new participant: {}", request.participantName());
        return repository.save(participant);
    }

    @Override
    public SupplyChainParticipant getParticipantById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + id));
    }

    @Override
    public List<SupplyChainParticipant> getAllParticipants() {
        return repository.findAll();
    }

    @Override
    public void updateParticipantStatus(UUID id, boolean isActive) {
        SupplyChainParticipant participant = getParticipantById(id); // Reuse method to handle 404
        participant.setActive(isActive);
        repository.save(participant);
        log.info("Updated status for participant {} to {}", id, isActive);
    }
}