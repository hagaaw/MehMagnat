package com.example.application.service;


import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private Logger logger = LoggerFactory.getLogger(ProductService.class);
    private ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }


    public void saveToDB(ProductEntity product){
        productRepository.save(product);
    }

    public List<ProductEntity> getAll() {
        return productRepository.findAll();
    }
}
