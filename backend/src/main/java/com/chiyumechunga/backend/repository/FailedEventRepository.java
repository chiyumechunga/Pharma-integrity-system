package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, UUID> {
    // You might need this for the retry job later
    long countByRetryCountLessThan(int maxRetries);

    List<FailedEvent> findByRetryCountLessThan(int i);
}