package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.provenance.FullProvenanceDto;
import com.chiyumechunga.backend.dto.provenance.ProvenanceResponseDto;

public interface ProvenanceService {
    ProvenanceResponseDto getProvenance(String qrHash);
}