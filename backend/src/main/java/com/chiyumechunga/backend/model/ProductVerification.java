package com.chiyumechunga.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaceuticalRegistry pharmaceuticalRegistry;

    // Maps the unit_id from the database to the SerializedUnit entity
    @ManyToOne
    @JoinColumn(name = "unit_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SerializedUnit serializedUnit;

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