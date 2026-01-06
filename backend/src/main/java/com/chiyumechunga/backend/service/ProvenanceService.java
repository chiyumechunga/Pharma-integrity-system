package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.provenance.FullProvenanceDto;

public interface ProvenanceService {
    FullProvenanceDto getProvenance(String qrHash);
}