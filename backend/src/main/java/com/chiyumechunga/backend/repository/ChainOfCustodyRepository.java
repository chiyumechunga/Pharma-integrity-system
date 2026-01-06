package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ChainOfCustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChainOfCustodyRepository extends JpaRepository<ChainOfCustodyEvent, UUID> {

    // It tells JPA: "Join with the Product table, filter by QR Hash, and sort by Time"
    List<ChainOfCustodyEvent> findByProduct_QrHashOrderByEventTimestampAsc(String qrHash);

    // (Optional) Helper for statistics if you used it elsewhere
    long countByProduct_RegistryId(UUID registryId);
}