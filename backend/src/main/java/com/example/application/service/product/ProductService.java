package com.example.application.service.product;



import com.example.application.entity.forProduct.ColorEntity;
import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forProduct.SizeEntity;
import com.example.application.repository.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


import static com.example.application.config.AppConfig.*;

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

    public void deleteBySku(String sku){
        productRepository.deleteBySku(sku);
    }

    public void saveNotNew(ProductEntity product){
        productRepository.save(product);
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
        product.setSku(generatedUniqueSku());
        productRepository.save(product);
    }



    public List<ProductEntity> getAll() {
        return productRepository.findAll();
    }

    public ProductEntity findBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден: " + sku));

    }

    private String generatedUniqueSku() {
        String sku;
        do {
            sku = generateRandomString(len);
        } while (productRepository.existsBySku(sku));
        return sku;
    }


}
