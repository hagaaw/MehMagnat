package com.example.application.repository.sale;

import com.example.application.entity.forSale.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    boolean existsBySaleNum(String saleNum);
}
