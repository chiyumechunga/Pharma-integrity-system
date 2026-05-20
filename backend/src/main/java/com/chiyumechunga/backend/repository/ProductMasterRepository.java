    package com.chiyumechunga.backend.repository;

    import com.chiyumechunga.backend.model.ProductMaster;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import java.util.Optional;
    import java.util.UUID;

    @Repository
    public interface ProductMasterRepository extends JpaRepository<ProductMaster, UUID> {

        Optional<ProductMaster> findByProductCode(String productCode);

        Optional<ProductMaster> findByGenericNameIgnoreCase(String genericName);
    }
