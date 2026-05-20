package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ChainOfCustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChainOfCustodyRepository extends JpaRepository<ChainOfCustodyEvent, UUID> {

    // 1. Standard JPA Method
    List<ChainOfCustodyEvent> findByRegistry_QrHashOrderByEventTimestampAsc(String qrHash);

    // 2. Helper for statistics
    long countByRegistry_RegistryId(UUID registryId);

    // --- NEW: Used for Dashboard Analytics ---
    long countByEventType(String eventType);

    // 3. Projection Interface to map the output of fn_get_product_provenance
    interface ProvenanceProjection {
        Date getEventTimestamp();
        String getEventType();
        Integer getQuantity();
        String getFromParticipantName();
        String getFromRole();
        String getToParticipantName();
        String getToRole();
        String getBlockchainTxId();
    }

    // 4. Native Query calling your PostgreSQL function
    @Query(nativeQuery = true, value = "SELECT * FROM fn_get_product_provenance(:qrHash)")
    List<ProvenanceProjection> getProductProvenance(@Param("qrHash") String qrHash);
}