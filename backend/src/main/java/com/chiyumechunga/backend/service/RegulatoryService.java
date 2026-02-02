package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;

public interface RegulatoryService {
    FireflyAckDto recordLabInspection(LabInspectionRequestDto request);
}