package com.chiyumechunga.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incident_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID reportId;

    @Column(name = "qr_hash")
    private String qrHash;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "reporter_contact")
    private String reporterPhoneOrEmail;

    @Column(name = "location_info")
    private String locationInfo;

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Column(name = "status")
    private String status; // PENDING_REVIEW, INVESTIGATING, RESOLVED

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;
}