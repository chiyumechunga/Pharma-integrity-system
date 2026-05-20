package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.CustodyTransferRequestDto; // <--- UPDATE THIS IMPORT
import com.chiyumechunga.backend.dto.FireflyAckDto;

import java.util.UUID;

public interface CustodyService {
    // Update the parameter type to match the DTO class we created
    FireflyAckDto transferCustody(CustodyTransferRequestDto request);
    /**
     * Dispenses a specific serial unit to an end consumer/patient.
     * Updates the local database state and records the de-aggregation
     * event on the immutable ledger.
     */
    FireflyAckDto dispenseUnit(String serialNumber, UUID pharmacyId);
}