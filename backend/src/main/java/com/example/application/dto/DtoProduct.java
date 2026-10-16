package com.example.application.dto;

import com.example.application.entity.forProduct.ProductEntity;
import lombok.Data;

@Data
public class DtoProduct {
    private String sku;
    private int count;
}
