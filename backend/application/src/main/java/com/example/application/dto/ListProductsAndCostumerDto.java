package com.example.application.dto;

import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forProduct.ProductEntity;
import lombok.Data;


import java.util.List;

@Data
public class ListProductsAndCostumerDto {
    private List<ProductEntity> products;
    private CustomerEntity customer;
}
