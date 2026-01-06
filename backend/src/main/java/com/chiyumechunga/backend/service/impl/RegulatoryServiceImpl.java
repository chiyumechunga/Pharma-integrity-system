package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegulatoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegulatoryServiceImpl implements RegulatoryService {

    private final FireflyIntegrationService fireflyService;

    public RegulatoryServiceImpl(FireflyIntegrationService fireflyService) {
        this.fireflyService = fireflyService;
    }

    @Override
    public FireflyAckDto submitInspection(LabInspectionRequestDto request) {
        log.info("Submitting Lab Inspection for Registry ID: {}", request.registryId());

        // 1. Submit to Blockchain (Firefly)
        // This invokes the "SubmitTestResult" function in your Chaincode
        String opId = fireflyService.invokeContract("SubmitTestResult", request);

        // 2. Return Pending Status
        // We do not write to the DB here. We wait for the blockchain event.
        return new FireflyAckDto(
                opId,
                "SUBMITTED",
                "Lab results submitted to blockchain for consensus."
        );
    }
}