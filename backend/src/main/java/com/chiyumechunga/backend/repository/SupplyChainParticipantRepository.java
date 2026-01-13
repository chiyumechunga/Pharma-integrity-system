package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.SupplyChainParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplyChainParticipantRepository extends JpaRepository<SupplyChainParticipant, UUID> {
    Optional<SupplyChainParticipant> findByParticipantCode(String participantCode);
    boolean existsByParticipantCode(String participantCode);
    Optional<SupplyChainParticipant> findByEmail(String email);

}