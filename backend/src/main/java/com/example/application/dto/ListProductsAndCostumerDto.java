package com.example.application.dto;

import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forProduct.ProductEntity;
import lombok.Data;


import java.time.LocalDate;
import java.util.List;

@Data
public class ListProductsAndCostumerDto {
    private LocalDate startAt;
    private LocalDate endAt;
    private List<DtoProduct> skuAndCount;
    private CustomerEntity customer;
}
