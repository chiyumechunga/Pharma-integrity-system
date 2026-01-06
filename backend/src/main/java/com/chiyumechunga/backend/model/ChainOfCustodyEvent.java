package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chain_of_custody_events")
@Data
@NoArgsConstructor
public class ChainOfCustodyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private UUID eventId;

    // RELATIONSHIP: Many events belong to One Product
    @ManyToOne
    @JoinColumn(name = "registry_id", nullable = false)
    private PharmaceuticalRegistry product;

    // RELATIONSHIP: The participant SENDING the product (Can be null if it's the Manufacturer creation event)
    @ManyToOne
    @JoinColumn(name = "from_participant_id")
    private SupplyChainParticipant fromParticipant;

    // RELATIONSHIP: The participant RECEIVING the product
    @ManyToOne
    @JoinColumn(name = "to_participant_id", nullable = false)
    private SupplyChainParticipant toParticipant;

    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g., "MANUFACTURED", "SHIPPED", "RECEIVED", "DISPENSED"

    @Column(name = "blockchain_tx_id", nullable = false)
    private String blockchainTxId; // The immutable proof from Hyperledger Firefly

    @CreationTimestamp
    @Column(name = "event_timestamp", updatable = false)
    private LocalDateTime eventTimestamp;
}