package com.chiyumechunga.backend.dto;

public class DispenseRequest {
    private String qrHash;
    private String pharmacyId;

    public String getQrHash() { return qrHash; }
    public void setQrHash(String qrHash) { this.qrHash = qrHash; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }
}
