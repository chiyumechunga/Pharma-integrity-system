package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.RegulatoryScrutiny;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.RegulatoryScrutinyRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.FireflyIntegrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RegulatoryServiceImplTest {

    @Mock
    private FireflyIntegrationService fireflyService;

    @Mock
    private RegulatoryScrutinyRepository scrutinyRepository;

    @Mock
    private SupplyChainParticipantRepository participantRepository;

    @InjectMocks
    private RegulatoryServiceImpl regulatoryService;

    @Test
    void shouldRecordInspectionWhenInspectorIsZAMRA() {
        // 1. SETUP
        UUID inspectorId = UUID.randomUUID();
        LabInspectionRequestDto request = new LabInspectionRequestDto(
                "BATCH-ZMB-999", inspectorId, true, "All chemical checks passed."
        );

        // Mock ZAMRA Inspector
        SupplyChainParticipant mockInspector = new SupplyChainParticipant();
        mockInspector.setParticipantId(inspectorId);
        mockInspector.setRole(ParticipantType.ZAMRA); // MUST BE ZAMRA

        // 2. MOCK
        Mockito.when(participantRepository.findById(inspectorId))
                .thenReturn(Optional.of(mockInspector));

        Mockito.when(scrutinyRepository.save(any(RegulatoryScrutiny.class)))
                .thenReturn(new RegulatoryScrutiny());

        Mockito.when(fireflyService.invokeContract(eq("RecordInspection"), eq(request), eq(ParticipantType.ZAMRA)))
                .thenReturn("ff-zamra-123");

        // 3. EXECUTE
        FireflyAckDto result = regulatoryService.recordLabInspection(request);

        // 4. VERIFY
        Assertions.assertEquals("SUBMITTED", result.status());
        Assertions.assertEquals("ff-zamra-123", result.operationId());

        // Verify DB save and Blockchain calls happened
        Mockito.verify(scrutinyRepository, Mockito.times(1)).save(any(RegulatoryScrutiny.class));
        Mockito.verify(fireflyService, Mockito.times(1)).invokeContract(any(), any(), eq(ParticipantType.ZAMRA));
    }

    @Test
    void shouldBlockInspectionWhenInspectorIsNOT_ZAMRA() {
        // 1. SETUP
        UUID pharmacyId = UUID.randomUUID();
        LabInspectionRequestDto request = new LabInspectionRequestDto(
                "BATCH-ZMB-999", pharmacyId, true, "I am a pharmacy trying to fake an inspection."
        );

        // Mock a regular pharmacy (NOT ZAMRA)
        SupplyChainParticipant mockPharmacy = new SupplyChainParticipant();
        mockPharmacy.setParticipantId(pharmacyId);
        mockPharmacy.setRole(ParticipantType.PHARMACY); // <--- WRONG ROLE

        Mockito.when(participantRepository.findById(pharmacyId))
                .thenReturn(Optional.of(mockPharmacy));

        // 2. EXECUTE & VERIFY
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            regulatoryService.recordLabInspection(request);
        });

        // Ensure the security message triggers
        Assertions.assertEquals("Unauthorized: Only ZAMRA can record inspections.", exception.getMessage());

        // Ensure Blockchain was NOT called
        Mockito.verify(fireflyService, Mockito.never()).invokeContract(any(), any(), any());
    }
}