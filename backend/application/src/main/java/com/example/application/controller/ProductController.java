package com.example.application.controller;


import com.example.application.dto.ProductRequest;
import com.example.application.entity.forProduct.*;
import com.example.application.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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


    @GetMapping("/all")
    public ResponseEntity<List<ProductEntity>> getProducts(){
        return ResponseEntity.ok(productService.getAll());
    }

    @PostMapping("/add")
    public ResponseEntity<ProductEntity> addProduct(
            @RequestParam String sku,
            @RequestParam String title,
            @RequestParam int quantityOnHand,
            @RequestParam int quantityReserved,
            @RequestParam BigDecimal price,

            @RequestBody ProductRequest productRequest

            ){
        ProductEntity product = new ProductEntity(sku,
                title, price, quantityOnHand, quantityReserved,
                LocalDateTime.now(),Status.FREE);
        product.setFurType(productRequest.getFurType());
        product.setColor(productRequest.getColor());
        product.setSize(productRequest.getSize());


        productService.saveToDB(product);


        return ResponseEntity.ok(product);
    }
}
