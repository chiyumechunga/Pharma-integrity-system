package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.TransferRequestDto;

public interface CustodyService {
    FireflyAckDto transferCustody(TransferRequestDto request);
}