package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supply_chain_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyChainParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "participant_code", unique = true, nullable = false)
    private String participantCode; // e.g., 'ZAMRA-001'

    @Column(name = "participant_name", nullable = false)
    private String participantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false)
    private ParticipantType participantType;

    @Column(name = "country")
    private String country;

    @Column(name = "blockchain_enrollment_id")
    private String blockchainEnrollmentId; // The Fabric/Ethereum Wallet Address

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}