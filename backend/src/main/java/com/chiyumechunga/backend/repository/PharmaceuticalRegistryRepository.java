package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmaceuticalRegistryRepository extends JpaRepository<PharmaceuticalRegistry, UUID> {
    Optional<PharmaceuticalRegistry> findByQrHash(String qrHash);
    boolean existsByQrHash(String qrHash);
    long countByCurrentStatus(String status);
    Optional<PharmaceuticalRegistry> findByBatchNumber(String batchNumber);
    boolean existsByBatchNumber(String batchNumber);

    // Dynamic Custody Resolution
    @Query(value = """
        WITH target_registry AS (
            -- 1. Grab the specific item using the scanned QR Hash
            SELECT registry_id, manufacturer_id 
            FROM pharmaceutical_registry 
            WHERE qr_hash = :scannedIdentifier 
            LIMIT 1
        ),
        latest_custody AS (
            -- 2. Find the most recent movement for this item
            SELECT to_participant_id 
            FROM chain_of_custody_events 
            WHERE registry_id = (SELECT registry_id FROM target_registry)
            ORDER BY event_timestamp DESC 
            LIMIT 1
        )
        SELECT EXISTS (
            SELECT 1 
            FROM target_registry t
            LEFT JOIN latest_custody c ON true
            WHERE 
                -- Condition A: The drug has moved before, and the sender was the last person to receive it
                (c.to_participant_id IS NOT NULL AND c.to_participant_id = :senderId)
                OR 
                -- Condition B: The drug has never moved, so the sender must be the original Manufacturer
                (c.to_participant_id IS NULL AND t.manufacturer_id = :senderId)
        )
    """, nativeQuery = true)
    boolean isBatchOwnedBy(@Param("scannedIdentifier") String scannedIdentifier, @Param("senderId") UUID senderId);
}