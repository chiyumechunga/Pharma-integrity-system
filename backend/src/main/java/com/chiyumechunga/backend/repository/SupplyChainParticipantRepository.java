package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.SupplyChainParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplyChainParticipantRepository extends JpaRepository<SupplyChainParticipant, UUID> {
    Optional<SupplyChainParticipant> findByParticipantCode(String participantCode);
    @Query("SELECT p.participantName FROM SupplyChainParticipant p WHERE p.blockchainEnrollmentId = :hash")
    Optional<String> findNameByBlockchainHash(String hash);
    boolean existsByParticipantCode(String participantCode);
    Optional<SupplyChainParticipant> findByEmail(String email);

}