package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.ProductRecall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRecallRepository extends JpaRepository<ProductRecall, UUID> {

    // 1. Find an active recall traversing the PharmaceuticalRegistry object
    Optional<ProductRecall> findByRegistry_RegistryIdAndStatus(UUID registryId, String status);

    // 2. Fetch all recalls currently active in the network
    List<ProductRecall> findByStatusOrderByRecallDateDesc(String status);

    // 3. Filter recalls by severity level (e.g., CLASS_I)
    List<ProductRecall> findBySeverityLevel(String severityLevel);

    // 4. Analytics: Count total active recalls for the dashboard
    long countByStatus(String status);

    // 5. Analytics: Get a breakdown of recalls by severity
    @Query("SELECT r.severityLevel, COUNT(r) FROM ProductRecall r WHERE r.status = 'ACTIVE' GROUP BY r.severityLevel")
    List<Object[]> countActiveRecallsBySeverity();
}