package com.chiyumechunga.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = SupplyChainParticipant.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "participant_id")
    private SupplyChainParticipant participant;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(String token, SupplyChainParticipant participant) {
        this.token = token;
        this.participant = participant;
        this.expiryDate = LocalDateTime.now().plusMinutes(15); // Token expires in 15 mins
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}