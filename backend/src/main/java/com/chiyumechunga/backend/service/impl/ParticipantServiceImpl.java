package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.ParticipantRegistrationDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException; // Ensure you have this exception
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.ParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ParticipantServiceImpl implements ParticipantService {

    private final SupplyChainParticipantRepository repository;

    public ParticipantServiceImpl(SupplyChainParticipantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SupplyChainParticipant registerParticipant(ParticipantRegistrationDto request) {
        log.info("Registering new participant: {}", request.participantName());

        // 1. Check for duplicates
        if (repository.existsByParticipantCode(request.participantCode())) {
            throw new DuplicateResourceException("Participant with code " + request.participantCode() + " already exists.");
        }

        // 2. Map DTO to Entity
        SupplyChainParticipant entity = new SupplyChainParticipant();
        entity.setParticipantCode(request.participantCode());
        entity.setParticipantName(request.participantName());
        entity.setParticipantType(request.participantType());
        entity.setCountry(request.country());
        entity.setBlockchainEnrollmentId(request.blockchainEnrollmentId());

        // 3. Save to DB
        return repository.save(entity);
    }
}