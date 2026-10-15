package com.example.application.repository.sale;

import com.example.application.entity.forSale.SaleEntity;
import com.example.application.entity.forSale.SaleItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItemEntity, Long> {
    void deleteAllBySale(SaleEntity sale);
}
