package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.RegulatoryScrutiny;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegulatoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryServiceImpl implements RegulatoryService {

    private final FireflyIntegrationService fireflyService;
    private final SupplyChainParticipantRepository participantRepository;
    private final RegulatoryScrutinyRepository scrutinyRepository;
    // Added Registry repository to link the product to the inspection
    private final PharmaceuticalRegistryRepository registryRepository;

    @Override
    public FireflyAckDto submitInspection(LabInspectionRequestDto request) {
        log.info("Verifying Inspector Identity for Registry ID: {}", request.registryId());

        // 1. SECURITY CHECK: Verify the inspector exists
        SupplyChainParticipant inspector = participantRepository.findById(request.inspectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspector ID not found."));

        // 2. ROLE-BASED ACCESS CONTROL (RBAC): Ensure only ZAMRA can do this
        if (inspector.getRole() != ParticipantType.ZAMRA) { // <--- Changed to getRole()
            log.warn("SECURITY ALERT: Unauthorized inspection attempt by participant ID: {}", request.inspectorId());
            throw new RuntimeException("Unauthorized: Only ZAMRA can record inspections.");
        }

        // 3. FETCH PRODUCT: Get the registry entry to attach to the scrutiny
        PharmaceuticalRegistry registry = registryRepository.findById(request.registryId())
                .orElseThrow(() -> new ResourceNotFoundException("Product registry ID not found."));

        // 4. LOG LOCALLY: Save inspection to off-chain DB
        RegulatoryScrutiny scrutiny = new RegulatoryScrutiny();
        scrutiny.setRegistry(registry); // Link the product
        scrutiny.setInspector(inspector); // Link the inspector
        scrutiny.setScrutinyDate(LocalDate.now()); // Fixed: uses LocalDate and correct setter
        scrutiny.setTestResult(request.testResult()); // Fixed: uses TestResult enum
        scrutiny.setLabNotes(request.labNotes()); // Fixed: uses labNotes
        scrutinyRepository.save(scrutiny);

        // 5. INVOKE BLOCKCHAIN: Route to Port 7003 (ZAMRA Node)
        String opId = fireflyService.invokeContract(
                "SubmitTestResult",
                request,
                ParticipantType.ZAMRA
        );

        return new FireflyAckDto(
                opId,
                "SUBMITTED",
                "Lab results verified and submitted to blockchain for consensus."
        );
    }
}