package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;

public interface RegistryService {
    // This defines the contract. Without this, @Override fails.
    FireflyAckDto registerBatch(RegistryRequestDto request);
}
