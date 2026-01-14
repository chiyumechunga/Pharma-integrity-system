package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_master")
@Data
@NoArgsConstructor
public class ProductMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;

    @Column(name = "generic_name", nullable = false)
    private String genericName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "strength")
    private String strength;

    @Column(name = "therapeutic_class")
    private String therapeuticClass;

    @Column(name = "requires_cold_chain")
    private boolean requiresColdChain;

    @Column(name = "approved_by_zamra")
    private boolean approvedByZamra;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}