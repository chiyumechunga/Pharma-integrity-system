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

    // FIX: Changed variable name from "product" to "registry".
    // Lombok @Data will now automatically create setRegistry() and getRegistry().
    @ManyToOne
    @JoinColumn(name = "registry_id", nullable = false)
    private PharmaceuticalRegistry registry;

    @ManyToOne
    @JoinColumn(name = "from_participant_id")
    private SupplyChainParticipant fromParticipant;

    @ManyToOne
    @JoinColumn(name = "to_participant_id", nullable = false)
    private SupplyChainParticipant toParticipant;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "blockchain_tx_id", nullable = false)
    private String blockchainTxId;

    @CreationTimestamp
    @Column(name = "event_timestamp", updatable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "quantity")
    private Integer quantity;

}