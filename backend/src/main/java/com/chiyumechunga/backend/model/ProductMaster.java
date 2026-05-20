package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Column(name = "max_units_per_batch", nullable = false)
    private Integer maxUnitsPerBatch = 20; // Default to 20 based on your DB schema

    @Column(name = "approved_by_zamra")
    private boolean approvedByZamra;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore //  Prevents the infinite loop when fetching batches
    @OneToMany(mappedBy = "product")
    private List<PharmaceuticalRegistry> batches;
}