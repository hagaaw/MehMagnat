package com.example.application.controller;


import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.service.FurTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fur-type")
public class FurTypeController {
    private FurTypeService furTypeService;

    @Autowired
    public FurTypeController(FurTypeService furTypeService){
        this.furTypeService = furTypeService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<FurTypeEntity>> getAllFurTypes(){
        return ResponseEntity.ok(furTypeService.getAllFurTypes());
    }
}
