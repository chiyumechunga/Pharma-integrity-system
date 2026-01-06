package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.RegulatoryScrutiny;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RegulatoryScrutinyRepository extends JpaRepository<RegulatoryScrutiny, UUID> {
    // Helper to find all failed tests (High Priority)
    long countByTestResult(String testResult);
}