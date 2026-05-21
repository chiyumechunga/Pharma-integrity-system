package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_recalls")
@Data
@NoArgsConstructor
public class ProductRecall {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "recall_id")
    private UUID recallId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_id")
    private PharmaceuticalRegistry registry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductMaster product;

    @Column(name = "recall_reason", nullable = false, columnDefinition = "TEXT")
    private String recallReason;

    @Column(name = "severity_level")
    private String severityLevel; // CLASS_I, CLASS_II, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private SupplyChainParticipant initiatedBy;

    @Column(name = "recall_date")
    private LocalDate recallDate = LocalDate.now();

    @Column(name = "status")
    private String status; // ACTIVE, COMPLETED, CANCELLED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
