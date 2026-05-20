package com.chiyumechunga.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "serialized_units")
public class SerializedUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "unit_id", updatable = false, nullable = false)
    private UUID unitId;

    @Column(name = "registry_id", nullable = false, insertable = false, updatable = false)
    private UUID registryId;    

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    // NEW: Added the qrHash field. Lombok will automatically create getQrHash() and setQrHash() for this!
    @Column(name = "qr_hash", length = 64)
    private String qrHash;

    @Column(name = "current_status")
    private String currentStatus = "IN_BATCH"; // Default status

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optional: Map the relationship to the parent batch
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_id")
    private PharmaceuticalRegistry batch;

}