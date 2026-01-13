package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ProductVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <--- Import this
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVerificationRepository extends JpaRepository<ProductVerification, UUID> {

    long countByIsValidTrue();
    long countByIsValidFalse();
    // 1. Matches your String schema (instead of boolean)
    long countByVerificationStatus(String verificationStatus);

    // 2. DETECT CLONES (Using Hibernate JPQL)
    // "Select the QR Hash from the Registry object inside Verification..."
    @Query("SELECT v.pharmaceuticalRegistry.qrHash, COUNT(v) " +
            "FROM ProductVerification v " +
            "GROUP BY v.pharmaceuticalRegistry.qrHash " +
            "HAVING COUNT(v) > 10")


    List<Object[]> findPotentialClones();
}