package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.service.FireflyIntegrationService;
import org.springframework.stereotype.Service; // <--- IMPORTS ARE CRITICAL
import com.chiyumechunga.backend.dto.RegistryRequestDto;
// ... other imports ...

@Service // <--- THIS ANNOTATION IS REQUIRED FOR AUTOWIRING
public class FireflyIntegrationServiceImpl implements FireflyIntegrationService {

    // ... the rest of your existing logic ...

    @Override
    public String invokeContract(String method, RegistryRequestDto request) {
        // ... your WebClient logic ...
        return "operation-id-placeholder"; // Ensure it returns a String
    }
}