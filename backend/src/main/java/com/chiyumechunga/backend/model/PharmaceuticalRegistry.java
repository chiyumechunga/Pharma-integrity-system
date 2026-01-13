package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "product_name")
    private String productName;

    @Column(name = "batch_number")
    private String batchNumber;

    // --- RELATIONSHIP FIX ---
    // Was: private UUID manufacturerId;
    // Now: Links to the Participant object so Hibernate can join tables
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private SupplyChainParticipant manufacturer;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "current_status")
    private String currentStatus;

    @Column(name = "blockchain_tx_id")
    private String blockchainTxId;

    @Column(name = "firefly_id")
    private String fireflyId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}