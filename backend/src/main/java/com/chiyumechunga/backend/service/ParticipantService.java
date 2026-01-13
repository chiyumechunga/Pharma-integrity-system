package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.ParticipantRegistrationDto;
import com.chiyumechunga.backend.model.SupplyChainParticipant;

import java.util.List;
import java.util.UUID;

public interface ParticipantService {

    // Registers a new user (hashing password, saving to DB)
    SupplyChainParticipant registerParticipant(ParticipantRegistrationDto request);

    // Fetches a single user (used for Profile view)
    SupplyChainParticipant getParticipantById(UUID id);

    // Lists all users (used for ZAMRA Dashboard)
    List<SupplyChainParticipant> getAllParticipants();

    // Updates status (used for approving/suspending users)
    void updateParticipantStatus(UUID id, boolean isActive);
}