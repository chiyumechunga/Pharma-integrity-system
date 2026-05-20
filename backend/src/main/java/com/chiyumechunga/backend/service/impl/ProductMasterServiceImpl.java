package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.dto.ProductRequestDto;
import com.chiyumechunga.backend.dto.ProductResponseDto;
import com.chiyumechunga.backend.exception.DuplicateResourceException;
import com.chiyumechunga.backend.exception.ResourceNotFoundException;
import com.chiyumechunga.backend.model.ProductMaster;
import com.chiyumechunga.backend.repository.ProductMasterRepository;
import com.chiyumechunga.backend.service.ProductMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for product_master operations.
 *
 * Main responsibilities:
 * 1. validate product uniqueness using product_code
 * 2. map ProductRequestDto to ProductMaster entity
 * 3. persist the entity to the database
 * 4. expose simple retrieval methods for controllers and other services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMasterServiceImpl implements ProductMasterService {

    private final ProductMasterRepository productMasterRepository;

    /**
     * Creates a new product catalog entry.
     */
    @Override
    @Transactional
    public ProductMaster registerProduct(ProductRequestDto request) {

        if (productMasterRepository.findByProductCode(request.productCode()).isPresent()) {
            throw new DuplicateResourceException(
                    "Product code already exists: " + request.productCode());
        }

        ProductMaster product = new ProductMaster();

        // Exact mappings to product_master columns
        product.setProductCode(request.productCode());
        product.setGenericName(request.genericName());
        product.setBrandName(request.brandName());
        product.setDosageForm(request.dosageForm());
        product.setStrength(request.strength());
        product.setTherapeuticClass(request.therapeuticClass());
        product.setRequiresColdChain(request.requiresColdChain());
        product.setApprovedByZamra(request.approvedByZamra());

        ProductMaster saved = productMasterRepository.save(product);

        log.info("Product registered successfully: code={}, id={}",
                saved.getProductCode(), saved.getProductId());

        return saved;
    }

    /**
     * Returns all products in the product catalog.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductMaster> getAllProducts() {
        return productMasterRepository.findAll();
    }

    /**
     * Retrieves one product using the primary key.
     */
    @Override
    @Transactional(readOnly = true)
    public ProductMaster getProductById(UUID productId) {
        return productMasterRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + productId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getPendingProducts() {
        // Fetch all products where approvedByZamra is false
        return productMasterRepository.findAll().stream()
                .filter(product -> !product.isApprovedByZamra())
                .map(this::mapToResponseDto) // FIXED: Now points to the helper method below
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS ---

    /**
     * Maps a ProductMaster entity to a ProductResponseDto.
     * Adjust the constructor arguments if your DTO record expects them in a different order.
     */
    private ProductResponseDto mapToResponseDto(ProductMaster product) {
        return new ProductResponseDto(
                product.getProductId(),
                product.getProductCode(),
                product.getGenericName(),
                product.getBrandName(),
                product.getDosageForm(),
                product.getStrength(),
                product.getTherapeuticClass(),
                product.isRequiresColdChain(),
                product.isApprovedByZamra(),
                product.getCreatedAt()
        );
    }
}