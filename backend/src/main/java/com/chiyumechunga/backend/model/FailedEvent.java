package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "failed_events")
@Data
@NoArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "failure_id")
    private UUID failureId;

    @Column(name = "tx_id")
    private String txId;

    @Column(name = "raw_payload", columnDefinition = "TEXT") // Store JSON as text
    private String rawPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt = LocalDateTime.now();
}