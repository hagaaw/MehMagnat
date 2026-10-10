package com.example.application.service;


import com.example.application.entity.forProduct.ColorEntity;
import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forProduct.SizeEntity;
import com.example.application.repository.ProductRepository;
import com.example.application.repository.SizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final FurTypeService furTypeService;
    private Logger logger = LoggerFactory.getLogger(ProductService.class);
    private ProductRepository productRepository;
    private ColorService colorService;
    private SizeService sizeService;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          ColorService colorService,
                          SizeService sizeService, FurTypeService furTypeService){
        this.productRepository = productRepository;
        this.colorService = colorService;
        this.sizeService = sizeService;
        this.furTypeService = furTypeService;
    }


    public void saveToDB(ProductEntity product){
        Optional<SizeEntity> size = sizeService.findBySize(product.getSize());
        if (size.isEmpty()){
            sizeService.save(product.getSize());
        }else {
            product.setSize(size.get());
        }

        Optional<FurTypeEntity> furType = furTypeService.findByFurType(product.getFurType());
        if (furType.isEmpty()){
            furTypeService.save(product.getFurType());
        }else {
            product.setFurType(furType.get());
        }

        Optional<ColorEntity> color = colorService.findByColor(product.getColor());
        if (color.isEmpty()){
            colorService.save(product.getColor());
        }else{
            product.setColor(color.get());
        }

        productRepository.save(product);
    }

    public List<ProductEntity> getAll() {
        return productRepository.findAll();
    }
}
