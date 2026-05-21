package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.LabInspectionRequestDto;
import com.chiyumechunga.backend.dto.RecallRequestDto;
import com.chiyumechunga.backend.model.ProductRecall;
import com.chiyumechunga.backend.model.RegulatoryScrutiny;

import java.util.List;
import java.util.UUID;

public interface RegulatoryService {


    FireflyAckDto recordLabInspection(LabInspectionRequestDto request);
    RegulatoryScrutiny getInspectionById(UUID id);
    void executeRecall(RecallRequestDto request);
    List<ProductRecall> getAllRecalls();
}