package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.RegulatoryScrutiny;
import com.chiyumechunga.backend.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegulatoryScrutinyRepository extends JpaRepository<RegulatoryScrutiny, UUID> {
    // Helper to find all failed tests (High Priority)
    long countByTestResult(TestResult testResult);

    // 2. MANUFACTURER QUALITY STATS (Using Hibernate JPQL)
    // "Navigate from Scrutiny to Manufacturer Name"
    @Query("SELECT r.registry.manufacturer.participantName, " +
            "       COUNT(r), " +
            "       SUM(CASE WHEN r.testResult = 'FAILED' THEN 1 ELSE 0 END) " +
            "FROM RegulatoryScrutiny r " +
            "GROUP BY r.registry.manufacturer.participantName")


    List<Object[]> getFailureRatesByManufacturer();
}