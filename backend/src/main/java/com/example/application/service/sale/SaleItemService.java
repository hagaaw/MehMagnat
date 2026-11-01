package com.example.application.service.sale;

import com.example.application.entity.forSale.SaleEntity;
import com.example.application.entity.forSale.SaleItemEntity;
import com.example.application.repository.sale.SaleItemRepository;
import com.example.application.repository.sale.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class SaleItemService {
    private final SaleItemRepository saleItemRepository;

    public SaleItemService(SaleItemRepository saleItemRepository) {
        this.saleItemRepository = saleItemRepository;
    }

    public void save(SaleItemEntity saleItem) {
        saleItemRepository.save(saleItem);
    }

    public void deleteAllBySale(SaleEntity sale) {
        saleItemRepository.deleteAllBySale(sale);
    }
}
