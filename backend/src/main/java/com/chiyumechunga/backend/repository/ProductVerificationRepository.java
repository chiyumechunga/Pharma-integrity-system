package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ProductVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVerificationRepository extends JpaRepository<ProductVerification, UUID> {

    // 1. Count by status (String)
    long countByVerificationStatus(String verificationStatus);

    // 2. Custom Analytics Query
    @Query("SELECT v.pharmaceuticalRegistry.qrHash, COUNT(v) " +
            "FROM ProductVerification v " +
            "GROUP BY v.pharmaceuticalRegistry.qrHash " +
            "HAVING COUNT(v) > 10")
    List<Object[]> findPotentialClones();
}