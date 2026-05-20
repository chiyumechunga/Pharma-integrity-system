package com.chiyumechunga.backend.repository;

import com.chiyumechunga.backend.model.SerializedUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerializedUnitRepository extends JpaRepository<SerializedUnit, UUID> {
    Optional<SerializedUnit> findBySerialNumber(String serialNumber);
    List<SerializedUnit> findByRegistryId(UUID registryId);

    Optional<SerializedUnit> findByQrHash(String qrHash);
}