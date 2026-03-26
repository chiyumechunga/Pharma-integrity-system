package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry; // <--- MISSING IMPORT

public interface RegistryService {
    // Defines the contract for creating a batch
    FireflyAckDto registerBatch(RegistryRequestDto request);

    // Defines the contract for retrieving a batch (FIXES THE ERROR)
    PharmaceuticalRegistry getBatchDetails(String batchNumber);
}