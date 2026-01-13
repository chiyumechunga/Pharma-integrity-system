package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_checkpoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCheckpoint {

    @Id
    @Column(name = "listener_id")
    private String listenerId; // e.g., "firefly-listener"

    @Column(name = "last_event_sequence")
    private String lastEventSequence; // The ID of the last event processed

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}