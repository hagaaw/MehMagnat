package com.example.application.controller;


import com.example.application.entity.forProduct.*;
import com.example.application.service.product.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name="products")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private ProductService productService;
    private final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    public ProductController(ProductService productService){
        this.productService = productService;
    }



    @PostMapping("/add")
    public ResponseEntity<ProductEntity> addProduct(
            @RequestBody ProductEntity product
            ){
        productService.saveToDB(product);
        return ResponseEntity.ok(product);
    }


    //свободных
    @GetMapping("/get_all")
    public ResponseEntity<List<ProductEntity>> getAllProducts(){
        var products = productService.getAll();
        if (products.isEmpty()){
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(productService.getAll());
    }
}
