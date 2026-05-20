package com.chiyumechunga.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pharmaceutical_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmaceuticalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "registry_id")
    private UUID registryId;

    @Column(name = "qr_hash", unique = true, nullable = false)
    private String qrHash;

    // --- NEW: LINK TO PRODUCT MASTER ---
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductMaster product;

    // We keep product_name as a cache/snapshot, or remove it if you prefer strict normalization
    @Column(name = "product_name")
    private String productName;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "batch_unit_count", nullable = false)
    private Integer batchUnitCount = 20;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private SupplyChainParticipant manufacturer;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "current_status")
    private String currentStatus;

    @Column(name = "blockchain_tx_id")
    private String blockchainTxId;

    @Column(name = "firefly_id")
    private java.util.UUID fireflyId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

}