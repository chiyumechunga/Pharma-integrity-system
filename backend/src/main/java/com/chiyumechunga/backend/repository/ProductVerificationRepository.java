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

    // Fixes "Cannot resolve method countByIsValid..."
    // NOTE: We switched to String status in the schema, so we use this:
    long countByVerificationStatus(String status);

    // Fixes "Cannot resolve method findPotentialClones"
    @Query("SELECT v.pharmaceuticalRegistry.qrHash, COUNT(v) FROM ProductVerification v GROUP BY v.pharmaceuticalRegistry.qrHash HAVING COUNT(v) > 10")
    List<Object[]> findPotentialClones();
}