package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "regulatory_scrutiny")
@Data
@NoArgsConstructor
public class RegulatoryScrutiny {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scrutiny_id")
    private UUID scrutinyId;

    // --- DUPLICATE MAPPING FIX ---
    // Removed 'private PharmaceuticalRegistry product;' to avoid crash.
    // We keep 'registry' because it matches the Service logic.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_id", nullable = false)
    private PharmaceuticalRegistry registry;

    @ManyToOne
    @JoinColumn(name = "inspector_id")
    private SupplyChainParticipant inspector;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_result")
    private TestResult testResult;

    @Column(name = "lab_notes", columnDefinition = "TEXT")
    private String labNotes;

    @Column(name = "scrutiny_date")
    private LocalDate scrutinyDate;

    @Column(name = "blockchain_tx_id")
    private String blockchainTxId;
}