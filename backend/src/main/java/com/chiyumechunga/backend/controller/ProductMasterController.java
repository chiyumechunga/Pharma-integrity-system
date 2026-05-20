package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.ProductRequestDto;
import com.chiyumechunga.backend.dto.ProductResponseDto;
import com.chiyumechunga.backend.model.ProductMaster;
import com.chiyumechunga.backend.service.ProductMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the product catalog.
 *
 * Endpoints:
 * POST /api/v1/products      -> create a product in product_master
 * GET  /api/v1/products      -> list all products
 * GET  /api/v1/products/{id} -> fetch product by UUID
 *
 * Why this controller exists:
 * A product must be created first before a batch can reference it using product_id.
 * That matches the intended relational flow in the schema.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductMasterController {

    private final ProductMasterService productMasterService;

    /**
     * Creates a new product.
     *
     * @Valid ensures DTO annotations are enforced before entering the service layer.
     */
    @PostMapping
    public ResponseEntity<ProductMaster> registerProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductMaster created = productMasterService.registerProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns all products in the catalog.
     */
    @GetMapping
    public ResponseEntity<List<ProductMaster>> getAllProducts() {
        return ResponseEntity.ok(productMasterService.getAllProducts());
    }

    /**
     * Returns a specific product by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductMaster> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productMasterService.getProductById(id));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ProductResponseDto>> getPendingApprovals() {
        return ResponseEntity.ok(productMasterService.getPendingProducts());
    }
}