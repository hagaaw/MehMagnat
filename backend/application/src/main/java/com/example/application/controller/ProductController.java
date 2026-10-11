package com.example.application.controller;


import com.example.application.entity.forProduct.*;
import com.example.application.service.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private ProductService productService;
    private final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    public ProductController(ProductService productService){
        this.productService = productService;
    }


    @GetMapping()
    public ResponseEntity<List<ProductEntity>> getProducts(){
        return ResponseEntity.ok(productService.getAll());
    }

    @PostMapping("/add")
    public ResponseEntity<ProductEntity> addProduct(
            @RequestBody ProductEntity product
            ){
        productService.saveToDB(product);
        return ResponseEntity.ok(product);
    }
}
