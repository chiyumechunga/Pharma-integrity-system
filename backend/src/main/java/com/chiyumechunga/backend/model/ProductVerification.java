package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_verification")
@Data
@NoArgsConstructor
public class ProductVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "verification_id")
    private UUID verificationId;

    @ManyToOne
    @JoinColumn(name = "registry_id")
    private PharmaceuticalRegistry pharmaceuticalRegistry;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "geo_location")
    private String geoLocation;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "scanned_by_role")
    private String scannedByRole;

    @CreationTimestamp
    @Column(name = "scan_timestamp")
    private LocalDateTime scanTimestamp;
}