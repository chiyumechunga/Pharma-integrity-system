package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ProductVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVerificationRepository extends JpaRepository<ProductVerification, UUID> {

    // 1. Used for Dashboard Stats
    long countByVerificationStatusNot(String status);

    // 2. Used by the Provenance Service
    Optional<ProductVerification> findTopByPharmaceuticalRegistry_RegistryIdOrderByScanTimestampDesc(UUID registryId);

    // 3. --- SUSPICIOUS ACTIVITY PROJECTION ---
    interface SuspiciousScanProjection {
        String getQrHash();
        long getScanCount();
        java.util.Date getLastScannedAt();
        long getFailedScans();
    }

    // 4. --- SUSPICIOUS ACTIVITY QUERY ---
    @Query("SELECT v.pharmaceuticalRegistry.qrHash as qrHash, " +
            "COUNT(v) as scanCount, " +
            "MAX(v.scanTimestamp) as lastScannedAt, " +
            "SUM(CASE WHEN v.verificationStatus != 'AUTHENTIC' THEN 1 ELSE 0 END) as failedScans " +
            "FROM ProductVerification v " +
            "GROUP BY v.pharmaceuticalRegistry.qrHash " +
            "HAVING COUNT(v) > 5 OR SUM(CASE WHEN v.verificationStatus != 'AUTHENTIC' THEN 1 ELSE 0 END) > 0 " +
            "ORDER BY lastScannedAt DESC")
    List<SuspiciousScanProjection> findSuspiciousScans();
}