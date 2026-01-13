package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supply_chain_participants")
public class SupplyChainParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "participant_code", unique = true, nullable = false)
    private String participantCode;

    @Column(name = "participant_name", nullable = false)
    private String participantName;

    // FIX FOR 'setRole' ERROR: Field name must match the setter (setRole -> role)
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false)
    private ParticipantType role;

    private String country;

    @Column(name = "blockchain_enrollment_id")
    private String blockchainEnrollmentId;

    // === FIX FOR 'setPassword', 'setActive', 'findByEmail' ERRORS ===
    @Column(unique = true)
    private String email;

    private String password;

    @Column(name = "is_active")
    private boolean isActive = true;
}