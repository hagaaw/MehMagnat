package com.example.application.repository.product;


import com.example.application.entity.forProduct.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    boolean existsBySku(String sku);

    Optional<ProductEntity> findBySku(String sku);

    void deleteBySku(String sku);
}
