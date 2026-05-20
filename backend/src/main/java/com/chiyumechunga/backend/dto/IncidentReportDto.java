package com.chiyumechunga.backend.dto;
import lombok.Data;

@Data
public class IncidentReportDto {
    private String qrHash; // The hash they scanned
    private String reporterName;
    private String reporterPhoneOrEmail;
    private String locationInfo; // Pharmacy name or Geo
    private String incidentDescription;
    private String issueType; // e.g., "COUNTERFEIT", "EXPIRED_SALE", "ALREADY_DISPENSED"
}