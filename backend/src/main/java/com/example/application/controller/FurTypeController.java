package com.example.application.controller;


import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.service.product.FurTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name="fur type")
@RestController
@RequestMapping("/api/fur-type")
public class FurTypeController {
    private FurTypeService furTypeService;

    @Autowired
    public FurTypeController(FurTypeService furTypeService){
        this.furTypeService = furTypeService;
    }

    @GetMapping("/get")
    public ResponseEntity<List<FurTypeEntity>> getAllFurTypes(){
        return ResponseEntity.ok(furTypeService.getAllFurTypes());
    }
}
