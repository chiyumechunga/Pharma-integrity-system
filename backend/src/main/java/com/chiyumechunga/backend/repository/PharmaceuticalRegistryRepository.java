package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmaceuticalRegistryRepository extends JpaRepository<PharmaceuticalRegistry, UUID> {

    Optional<PharmaceuticalRegistry> findByQrHash(String qrHash);
    boolean existsByQrHash(String qrHash);

    long countByCurrentStatus(String confirmed);


    Optional<PharmaceuticalRegistry> findByBatchNumber(String batchNumber); // NEW: For duplicate check


    boolean existsByBatchNumber(String batchNumber); // NEW: Alternative duplicate check



}