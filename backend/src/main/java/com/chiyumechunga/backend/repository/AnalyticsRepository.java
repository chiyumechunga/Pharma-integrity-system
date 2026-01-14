package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.dto.DashboardStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry; // Dummy entity for generic repo

import java.util.UUID;
import java.util.List;
import java.util.Map;

@Repository
public interface AnalyticsRepository extends JpaRepository<PharmaceuticalRegistry, UUID> {

    // 1. Fetch the Dashboard View directly as a Map
    @Query(value = "SELECT * FROM vw_poc_dashboard", nativeQuery = true)
    Map<String, Object> getDashboardStats();

    // 2. Fetch Suspicious Patterns View
    @Query(value = "SELECT * FROM vw_suspicious_patterns", nativeQuery = true)
    List<Map<String, Object>> getSuspiciousPatterns();

    // 3. Fetch Verification Trends
    @Query(value = "SELECT * FROM vw_verification_trends", nativeQuery = true)
    List<Map<String, Object>> getVerificationTrends();
}