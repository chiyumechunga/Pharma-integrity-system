package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.EventCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCheckpointRepository extends JpaRepository<EventCheckpoint, String> {
    // Basic CRUD is enough
}