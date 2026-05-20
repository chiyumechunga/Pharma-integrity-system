package com.chiyumechunga.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "registry_id", nullable = false)
    private PharmaceuticalRegistry registry;

    // NEW: Map the specific item-level unit
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "unit_id")
    private SerializedUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "from_participant_id")
    private SupplyChainParticipant fromParticipant;

    // REMOVED 'nullable = false'. A patient receiving a dispensed drug is not a tracked participant.
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "to_participant_id")
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