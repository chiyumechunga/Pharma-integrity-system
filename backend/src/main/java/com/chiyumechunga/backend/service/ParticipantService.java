package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.ParticipantRegistrationDto;
import com.chiyumechunga.backend.model.SupplyChainParticipant;

public interface ParticipantService {
    SupplyChainParticipant registerParticipant(ParticipantRegistrationDto request);
}