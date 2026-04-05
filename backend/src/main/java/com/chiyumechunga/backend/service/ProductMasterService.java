package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.ProductRequestDto;
import com.chiyumechunga.backend.model.ProductMaster;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for the product catalog.
 *
 * This is intentionally separate from RegistryService because:
 * - creating a product is not the same action as registering a batch
 * - a product belongs to product_master
 * - a batch belongs to pharmaceutical_registry
 * - one product can have many batches
 */
public interface ProductMasterService {

    ProductMaster registerProduct(ProductRequestDto request);

    List<ProductMaster> getAllProducts();

    ProductMaster getProductById(UUID productId);
}