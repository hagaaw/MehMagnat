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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



import static com.example.application.config.AppConfig.*;

@Service
@Transactional
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
        checkForUniqueForDB(product);
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


    public ProductEntity putProduct(String sku, ProductEntity product) {
        ProductEntity entity = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));


        SizeEntity resolvedSize = sizeService.findBySize(product.getSize())
                .orElseGet(() -> sizeService.save(product.getSize()));
        FurTypeEntity resolvedFurType = furTypeService.findByFurType(product.getFurType())
                .orElseGet(() -> furTypeService.save(product.getFurType()));
        ColorEntity resolvedColor = colorService.findByColor(product.getColor())
                .orElseGet(() -> colorService.save(product.getColor()));

        entity.setQuantityOnHand(product.getQuantityOnHand());
        entity.setQuantityReserved(product.getQuantityReserved());
        entity.setStatus(product.getStatus());
        entity.setTitle(product.getTitle());
        entity.setListPrice(product.getListPrice());

        entity.setSize(resolvedSize);
        entity.setFurType(resolvedFurType);
        entity.setColor(resolvedColor);

        return productRepository.save(entity);
    }

    private void checkForUniqueForDB(ProductEntity product) {
        if (product.getSize() == null)    throw new IllegalArgumentException("size is required");
        if (product.getFurType() == null) throw new IllegalArgumentException("furType is required");
        if (product.getColor() == null)   throw new IllegalArgumentException("color is required");

        product.setSize(sizeService.findBySize(product.getSize())
                .orElseGet(() -> sizeService.save(product.getSize())));
        product.setFurType(furTypeService.findByFurType(product.getFurType())
                .orElseGet(() -> furTypeService.save(product.getFurType())));
        product.setColor(colorService.findByColor(product.getColor())
                .orElseGet(() -> colorService.save(product.getColor())));
    }
}
