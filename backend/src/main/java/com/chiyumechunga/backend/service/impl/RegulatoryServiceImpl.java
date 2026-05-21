package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.dto.RecallRequestDto;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.ProductRecall;
import com.chiyumechunga.backend.model.RegulatoryScrutiny;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.ProductRecallRepository;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import com.chiyumechunga.backend.service.RegulatoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryServiceImpl implements RegulatoryService {

    private final FireflyIntegrationService fireflyService;
    private final SupplyChainParticipantRepository participantRepository;
    private final RegulatoryScrutinyRepository scrutinyRepository;
    private final PharmaceuticalRegistryRepository registryRepository;
    private final ProductRecallRepository recallRepository;

    @Override
    @Transactional
    public FireflyAckDto recordLabInspection(LabInspectionRequestDto request) {
        log.info("Verifying Inspector Identity for Registry ID: {}", request.registryId());

        // 1. SECURITY CHECK
        SupplyChainParticipant inspector = participantRepository.findById(request.inspectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspector ID not found."));

        // 2. RBAC
        if (inspector.getRole() != ParticipantType.ZAMRA) {
            log.warn("SECURITY ALERT: Unauthorized inspection attempt by participant ID: {}", request.inspectorId());
            throw new RuntimeException("Unauthorized: Only ZAMRA can record inspections.");
        }

        // 3. FETCH PRODUCT
        PharmaceuticalRegistry registry = registryRepository.findById(request.registryId())
                .orElseThrow(() -> new ResourceNotFoundException("Product registry ID not found."));

        // 4. LOG LOCALLY
        RegulatoryScrutiny scrutiny = new RegulatoryScrutiny();
        scrutiny.setRegistry(registry);
        scrutiny.setInspector(inspector);
        scrutiny.setScrutinyDate(LocalDate.now());
        scrutiny.setTestResult(request.testResult());
        scrutiny.setLabNotes(request.labNotes());
        scrutinyRepository.save(scrutiny);

        // 5. INVOKE BLOCKCHAIN
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

    @Override
    @Transactional(readOnly = true)
    public RegulatoryScrutiny getInspectionById(UUID id) {
        return scrutinyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutiny record not found."));
    }

    @Override
    @Transactional
    public void executeRecall(RecallRequestDto request) {
        log.info("Executing market recall for batch: {}", request.batchNumber());

        // 1. Fetch the target batch
        PharmaceuticalRegistry batch = registryRepository.findByBatchNumber(request.batchNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.batchNumber()));

        // 2. Validate the initiator's authority
        SupplyChainParticipant initiator = participantRepository.findById(request.initiatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Initiator not found."));

        if (initiator.getRole() != ParticipantType.ZAMRA) {
            log.warn("SECURITY ALERT: Unauthorized recall attempt by: {}", initiator.getParticipantId());
            throw new RuntimeException("Unauthorized: Only ZAMRA can initiate recalls.");
        }

        // 3. Create the official recall record using Object relationships
        ProductRecall recall = new ProductRecall();
        recall.setRegistry(batch);               // Maps to registry_id
        recall.setProduct(batch.getProduct());   // Maps to product_id
        recall.setRecallReason(request.recallReason());
        recall.setSeverityLevel(request.severityLevel());
        recall.setInitiatedBy(initiator);        // Maps to initiated_by
        recall.setStatus("ACTIVE");

        recallRepository.save(recall);

        // 4. Update the Batch Status globally
        batch.setCurrentStatus("RECALLED");
        registryRepository.save(batch);

        log.info("Batch {} status updated to RECALLED.", batch.getBatchNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecall> getAllRecalls() {
        return recallRepository.findAll();
    }
}