package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto; // <--- UPDATE THIS IMPORT
import com.chiyumechunga.backend.dto.FireflyAckDto;

public interface CustodyService {
    // Update the parameter type to match the DTO class we created
    FireflyAckDto transferCustody(CustodyTransferRequestDto request);
}